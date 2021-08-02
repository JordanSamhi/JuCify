import os
import sys
import argparse
import timeit
import tempfile
import multiprocessing as mp
import multiprocessing.pool
import threading
import angr
import cle
import pydot
import networkx as nx
import logging
from androguard.misc import AnalyzeAPK

from import_implementations import hookAllImportSymbols
from jni_interfaces.record import Record
from jni_interfaces.utils import (record_static_jni_functions, clean_records,
        record_dynamic_jni_functions, print_records, analyze_jni_function,
        jni_env_prepare_in_object, JNI_LOADER)

ANGR_RETDEC_OFFSET = 4194305

# the longest time in seconds to analyze 1 JNI function.
WAIT_TIME = 180
# the longest time in seconds for dynamic registration analysis
DYNAMIC_ANALYSIS_TIME = 600

# Directory for different ABIs, refer to: https://developer.android.com/ndk/guides/abis
ABI_DIRS = ['lib/armeabi-v7a/', 'lib/armeabi/', 'lib/arm64-v8a/', 'lib/x86/', 'lib/x86_64/']
FDROID_DIR = '../fdroid_crawler'
NATIVE_FILE = os.path.join(FDROID_DIR, 'natives')
# OUT_DIR = 'fdroid_result'
OUT_DIR = os.path.expandvars('$SCRATCH/native_lin')

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)

# uncomment below to output log information
# logging.disable(level=logging.CRITICAL)


class Performance:
    def __init__(self):
        self._start_at = None
        self._end_at = None
        self._num_analyzed_func = 0
        self._num_analyzed_so = 0
        self._num_timeout = 0
        self._dynamic_func_reg_analysis_timeout = 0

    def start(self):
        self._start_at = timeit.default_timer()

    def end(self):
        self._end_at = timeit.default_timer()

    def add_analyzed_func(self):
        self._num_analyzed_func += 1

    def add_analyzed_so(self):
        self._num_analyzed_so += 1

    def add_timeout(self):
        self._num_timeout += 1

    def add_dynamic_reg_timeout(self):
        self._dynamic_func_reg_analysis_timeout += 1

    @property
    def elapsed(self):
        if self._start_at is None or self._end_at is None:
            return None
        else:
            return self._end_at - self._start_at

    def __str__(self):
        s = 'elapsed,analyzed_so,analyzed_func,func_timeout,dymamic_timeout\n'
        s += f'{self.elapsed},{self._num_analyzed_so},{self._num_analyzed_func},{self._num_timeout},{self._dynamic_func_reg_analysis_timeout}'
        return s


class NoDaemonProcess(mp.Process):
    # make 'daemon' attribute always return False
    def _get_daemon(self):
        return False
    def _set_daemon(self, value):
        pass
    daemon = property(_get_daemon, _set_daemon)


# Make Pool with none daemon process in order to have children process.
# We sub-class multiprocessing.pool.Pool instead of multiprocessing.Pool
# because the latter is only a wrapper function, not a proper class.
class MyPool(multiprocessing.pool.Pool):
    Process = NoDaemonProcess


def main():
    cmd()
    # fdroid_run()
    # lineage_run()
    # check_duplicates()


def check_duplicates():
    apks = get_native_apks()
    print(len(apks))
    names = [apk.split('/')[-1] for apk in apks]
    duplicates = [n for n in names if names.count(n) > 1]
    print(len(duplicates))
    print(duplicates)
    print(len(set(names)))


def fdroid_run():
    apks = get_native_apks()
    apks = filter_out_exists(apks)
    if not os.path.exists(OUT_DIR):
        os.makedirs(OUT_DIR)
    with MyPool() as p:
        p.map(apk_run, apks)


def lineage_run():
    lin_file = sys.argv[1]
    shas = list()
    with open(lin_file) as f:
        for l in f:
            l = l.strip()
            shas.append(l)
    if not os.path.exists(OUT_DIR):
        os.makedirs(OUT_DIR)
    with MyPool() as p:
        p.map(sha_run, shas)


def filter_out_exists(apks):
    if not os.path.exists(OUT_DIR):
        return apks
    exists = list()
    for i in os.listdir(OUT_DIR):
        if i.endswith('_result'):
            exists.append(i.rstrip('_result'))
    noexists = list()
    for apk in apks:
        ne = True
        name = apk.split('/')[-1]
        for e in exists:
            if name.rstrip('.apk') == e:
                ne = False
                break
        if ne:
            noexists.append(apk)
    return noexists


def get_native_apks():
    apks = list()
    with open(NATIVE_FILE) as f:
        for l in f:
            apk = l.split(',')[0]
            apks.append(os.path.join(FDROID_DIR, apk))
    return apks


def cmd():
    path_2_apk, out, output_cg = parse_args()
    apk_run(path_2_apk, out, output_cg)


def print_performance(perf, out):
    file_name = os.path.join(out, 'performance')
    with open(file_name, 'w') as f:
        print(perf, file=f)


def parse_args():
    desc = 'Analysis APKs for native and Java inter-invocations'
    parser = argparse.ArgumentParser(description=desc)
    parser.add_argument('apk', type=str, help='directory to the APK file')
    parser.add_argument('--out', type=str, default=None, help='the output directory')
    parser.add_argument('--cg', help='Enable the output of binary callgraph as a dot file', action='store_true')
    args = parser.parse_args()
    if not os.path.exists(args.apk):
        print('APK file does not exist!', file=sys.stderr)
        sys.exit(-1)
    if args.out is None:
        # output locally with the same name of the apk.
        args.out = '.'
    basename = os.path.basename(args.apk)
    without_extension = os.path.splitext(basename)[0]
    result_dir = f"{without_extension}_result"
    out = os.path.join(args.out, result_dir)
    if not os.path.exists(out):
        os.makedirs(out)
    return args.apk, out, args.cg


def sha_run(sha):
    from mylib.androzoo import download
    sucess, desc, _ = download(sha, '/tmp', False)
    if sucess:
        try:
            apk_run(desc, comprise=True)
        except Exception as e:
            print(sha, 'failed with error:', e, file=sys.stderr)
        try:
            os.remove(desc)
        except Exception as e:
            print(f'remove {sha} failed: {e}', file=sys.stderr)
    else:
        print(f'download {sha} failed: {desc}', file=sys.stderr)


def select_abi_dir(dir_list):
    selected = None
    abis = set()
    for n in dir_list:
        if n.startswith('lib/'):
            abis.add(n.split('/')[1])
    for abi_dir in ABI_DIRS:
        abi = abi_dir.split('/')[1]
        if abi in abis:
            selected = abi_dir
            break
    return selected


def get_return_address(state):
    # TODO: check architecture to decide from where to retrieve return address.
    return_addr = None
    # for ARM, get it from register, lr
    if 'ARM' in state.arch.name:
        return_addr = state.solver.eval(state.regs.lr)
    else:
        logger.warning(f'Retrieve return address of architecture {state.arch.name} has not been implemented!')
    return return_addr


def cg_addr_hook(state):
    addr = state.addr
    func_info = state.globals.get('func_info')
    func_stack = state.globals.get('func_stack')
    if len(func_stack) == 0:
        # entering a tracking function
        func = find_func(addr, func_info, 'enter')
        if func is not None:
            logger.debug(f'enter func: {func}')
            func_stack.append(func)
            info = func_info.get(func)
            # appending and hooking func ending address for judging exiting of the func.
            return_addr = get_return_address(state)
            info.append(return_addr)
            # as func_info is a dict proxy, the list object have always to be updated.
            func_info.update({func: info})
            state.project.hook(return_addr, hook=cg_addr_hook)
    else:
        # check if exiting current function
        addrs = func_info.get(func_stack[-1])
        if len(addrs) == 2 and addrs[1] == addr:
            exit_func = func_stack.pop()
            logger.debug(f'exit func: {exit_func}')
        # check if enterring a new function
        func = find_func(addr, func_info, 'enter')
        if func is not None:
            logger.debug(f'enter func: {func}')
            func_stack.append(func)
            info = func_info.get(func)
            # appending and hooking func ending address for judging exiting of the func.
            return_addr = get_return_address(state)
            info.append(return_addr)
            # as func_info is a dict proxy, the list object have always to be updated.
            func_info.update({func: info})
            state.project.hook(return_addr, hook=cg_addr_hook)


def find_func(addr, f_info, addr_type):
    types = ('enter', 'exit')
    the_func = None
    if not addr_type in types:
        logger.warning(f'"find_func" does not support the "addr_type": {addr_type}!')
        return the_func
    for func, addrs in f_info.items():
        if addr_type == types[0]:
            func_addr = addrs[0]
        else:
            func_addr = addrs[1] if len(addrs) == 2 else None
        if addr == func_addr:
            the_func = func
            break
    return the_func



def get_function_addresses(proj, output_cg=False, path=None):
    funcs_addrs = list()
    cfg = proj.analyses.CFGFast()
    for addr in cfg.functions:
        f = cfg.functions[addr]
        if not f.is_simprocedure and not f.is_syscall and not f.is_plt and not proj.is_hooked(addr):
            funcs_addrs.append((f.name, addr))
    if output_cg:
        file_name_cg = proj.filename.split('/')[-1] + '.dot'
        file_name_map = proj.filename.split('/')[-1] + '.map'
        path = '.' if path is None else path
        if not os.path.exists(path):
            os.makedirs(path)
        cg_path = os.path.join(path, file_name_cg)
        map_path = os.path.join(path, file_name_map)
        # output the function name to address mapping. Since in CG, only addresses are provided.
        with open(map_path, 'w') as f:
            for func, addr in funcs_addrs:
                print(f'{func}:{addr}', file=f)
        # output the CG as a dot file. Can use the "dot" command of Graphviz access.
        dot = nx.nx_pydot.to_pydot(cfg.functions.callgraph)
        dot.write(cg_path)
    return funcs_addrs


def apk_run(path, out=None, output_cg=False, comprise=False):
    perf = Performance()
    if out is None:
        result_dir = path.split('/')[-1].rstrip('.apk') + '_result'
        out = os.path.join(OUT_DIR, result_dir)
        if not os.path.exists(out):
            os.makedirs(out)
    perf.start()
    apk, _, dex = AnalyzeAPK(path)
    with apk.zip as zf, tempfile.TemporaryDirectory() as tmpd:
        chosen_abi_dir = select_abi_dir(zf.namelist())
        if chosen_abi_dir is None:
            logger.debug(f'No ABI directories were found for .so file in {path}')
            return
        logger.debug(f'Use shared library (i.e., .so) files from {chosen_abi_dir}')
        for n in zf.namelist():
            if n.endswith('.so') and n.startswith(chosen_abi_dir):
                logger.debug(f'Start to analyze {n}')
                so_file = zf.extract(n, path=tmpd)
                with mp.Manager() as mgr:
                    returns = mgr.dict()
                    proj, jvm, jenv, dynamic_timeout = find_all_jni_functions(so_file, dex)
                    if proj is None:
                        logger.warning(f'Project object generation failed for {n}')
                        continue
                    if dynamic_timeout:
                        perf.add_dynamic_reg_timeout()
                    func_info = dict()
                    funcs_addrs = get_function_addresses(proj, output_cg, out)
                    for func, addr in funcs_addrs:
                        proj.hook(addr, hook=cg_addr_hook)
                        func_info.update({func:[addr]})
                    global_refs = {
                        'func_info': mgr.dict(func_info),
                        'func_stack': mgr.list()
                    }
                    perf.add_analyzed_so()
                    for jni_func, record in Record.RECORDS.items():
                        # clear func stack before each analysis
                        global_refs.get('func_stack')[:] = list()
                        # wrap the analysis with its own process to limit the
                        # analysis time.
                        p = mp.Process(target=analyze_jni_function,
                                args=(*(jni_func, proj, jvm, jenv, dex, returns, global_refs),))
                        p.start()
                        perf.add_analyzed_func()
                        # For analysis of each .so file, we wait for 3mins at most.
                        p.join(WAIT_TIME)
                        if p.is_alive():
                            perf.add_timeout()
                            p.terminate()
                            p.join()
                            logger.warning(f'Timeout when analyzing {n}')
                    for addr, invokees in returns.items():
                        record = Record.RECORDS.get(addr)
                        for invokee in invokees:
                            record.add_invokee(invokee)
                    file_name = n.split('/')[-1] + '.result'
                    print_records(os.path.join(out, file_name))
    perf.end()
    print_performance(perf, out)
    if comprise:
        from mylib.common import zipdir
        zipdir(result_dir, out, OUT_DIR, True)


def refactor_cls_name(raw_name):
    return raw_name.lstrip('L').rstrip(';').replace('/', '.')


def find_all_jni_functions(so_file, dex):
    proj, jvm_ptr, jenv_ptr = None, None, None
    # Mark whether the analysis for dynamic registration is timeout.
    dynamic_analysis_timeout = False
    try:
        proj = angr.Project(so_file, auto_load_libs=False)
    except Exception as e:
        logger.warning(f'{so_file} cause angr loading error: {e}')
    else:
        hookAllImportSymbols(proj)
        jvm_ptr, jenv_ptr = jni_env_prepare_in_object(proj)
        clean_records()
        record_static_jni_functions(proj, dex)
        if proj.loader.find_symbol(JNI_LOADER):
            # wrap the analysis with its own process to limit the analysis time.
            with mp.Manager() as mgr:
                records = mgr.dict()
                p = mp.Process(target=record_dynamic_jni_functions,
                        args=(*(proj, jvm_ptr, jenv_ptr, dex, records),))
                p.start()
                p.join(DYNAMIC_ANALYSIS_TIME)
                if p.is_alive():
                    dynamic_analysis_timeout = True
                    p.terminate()
                    p.join()
                    logger.warning('Timeout when analyzing dynamic registration')
                Record.RECORDS.update(records)
    return proj, jvm_ptr, jenv_ptr, dynamic_analysis_timeout


if __name__ == '__main__':
    main()

