import os
import sys
import argparse
import timeit
import multiprocessing as mp
import multiprocessing.pool
import threading
import angr
import cle
import logging
from androguard.misc import AnalyzeAPK

from jni_interfaces.record import Record
from jni_interfaces.utils import (record_static_jni_functions, clean_records,
        record_dynamic_jni_functions, print_records, analyze_jni_function,
        jni_env_prepare_in_object, JNI_LOADER)

SO_DIR = 'lib/armeabi-v7a/'
FDROID_DIR = '../fdroid_crawler'
NATIVE_FILE = os.path.join(FDROID_DIR, 'natives')
# OUT_DIR = 'fdroid_result'
OUT_DIR = os.path.expandvars('$SCRATCH/native_lin')

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)

logging.disable(level=logging.CRITICAL)


class Performance:
    def __init__(self):
        self._start_at = None
        self._end_at = None
        self._num_analyzed_func = 0
        self._num_analyzed_so = 0
        self._num_timeout = 0

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

    @property
    def elapsed(self):
        if self._start_at is None or self._end_at is None:
            return None
        else:
            return self._end_at - self._start_at

    def __str__(self):
        s = 'elapsed,analyzed_so,analyzed_func,timeout\n'
        s += f'{self.elapsed},{self._num_analyzed_so},{self._num_analyzed_func},{self._num_timeout}'
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
    #lineage_run()
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
    path_2_apk, out = parse_args()
    apk_run(path_2_apk, out)


def print_performance(perf, out):
    file_name = os.path.join(out, 'performance')
    with open(file_name, 'w') as f:
        print(perf, file=f)


def parse_args():
    desc = 'Analysis APKs for native and Java inter-invocations'
    parser = argparse.ArgumentParser(description=desc)
    parser.add_argument('apk', type=str, help='directory to the APK file')
    parser.add_argument('--out', type=str, default=None, help='the output directory')
    args = parser.parse_args()
    if not os.path.exists(args.apk):
        print('APK file does not exist!', file=sys.stderr)
        sys.exit(-1)
    if args.out is None:
        # output locally with the same name of the apk.
        args.out = '.'
    result_dir = args.apk.split('/')[-1].rstrip('.apk') + '_result'
    out = os.path.join(args.out, result_dir)
    if not os.path.exists(out):
        os.makedirs(out)
    return args.apk, out


def sha_run(sha):
    from mylib.androzoo import download
    sucess, desc, _ = download(sha, '/tmp', False)
    if sucess:
        apk_run(desc, comprise=True)
        try:
            os.remove(desc)
        except Exception as e:
            print(f'remove {sha} failed: {e}', file=sys.stderr)
    else:
        print(f'download {sha} failed: {desc}', file=sys.stderr)


def apk_run(path, out=None, comprise=False):
    perf = Performance()
    if out is None:
        result_dir = path.split('/')[-1].rstrip('.apk') + '_result'
        out = os.path.join(OUT_DIR, result_dir)
        if not os.path.exists(out):
            os.makedirs(out)
    perf.start()
    apk, _, dex = AnalyzeAPK(path)
    with apk.zip as zf:
        for n in zf.namelist():
            if n.startswith(SO_DIR) and n.endswith('.so'):
                # print('='*100, n)
                with zf.open(n) as so_file, mp.Manager() as mgr:
                    returns = mgr.dict()
                    proj, jvm, jenv = find_all_jni_functions(so_file, dex)
                    if proj is None:
                        continue
                    perf.add_analyzed_so()
                    for jni_func, record in Record.RECORDS.items():
                        # wrap the analysis with its own process to limit the
                        # analysis time.
                        # print(record.symbol_name)
                        p = mp.Process(target=analyze_jni_function,
                                args=(*(jni_func, proj, jvm, jenv, dex, returns),))
                        p.start()
                        perf.add_analyzed_func()
                        # For analysis of each .so file, we wait for 3mins at most.
                        p.join(180)
                        if p.is_alive():
                            perf.add_timeout()
                            p.terminate()
                            p.join()
                            # print('timeout')
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
    try:
        cle_loader = cle.loader.Loader(so_file, auto_load_libs=False)
    except Exception as e:
        logger.warning(f'{so_file} cause CLE loader error: {e}')
    else:
        proj = angr.Project(cle_loader)
        jvm_ptr, jenv_ptr = jni_env_prepare_in_object(proj)
        clean_records()
        record_static_jni_functions(proj, dex)
        if proj.loader.find_symbol(JNI_LOADER):
            # print('record dynamic', '-'*50)
            record_dynamic_jni_functions(proj, jvm_ptr, jenv_ptr, dex)
    return proj, jvm_ptr, jenv_ptr


if __name__ == '__main__':
    main()

