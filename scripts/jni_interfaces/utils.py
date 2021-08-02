import sys
import re
import logging
import traceback
from claripy import BVS
from angr.sim_type import register_types, parse_type
from angr.exploration_techniques import LengthLimiter

from . import JNI_PROCEDURES
from .common import NotImplementedJNIFunction, JavaClass
from .jni_invoke import jni_invoke_interface as jvm
from .jni_native import jni_native_interface as jenv
from .record import Record, RecordNotFoundError

JNI_LOADER = 'JNI_OnLoad'
# value for "LengthLimiter" to limit the length of path a state goes through.
# refer to: https://docs.angr.io/core-concepts/pathgroups
MAX_LENGTH = 500000
DYNAMIC_ANALYSIS_LENGTH = 100000

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)


def record_static_jni_functions(proj, dex=None):
    """record the statically exported JNI functions
    The strategy is to 1st find the symbol names started with 'Java',
    2nd verify the truth of JNI function by check the class part of the name
    with classes in the 'cls_list' which is from dex files of the APK.
    """
    if dex is not None:
        native_methods = [m for m in dex.get_methods() if 'native' in m.access]
    for s in proj.loader.symbols:
        if s.name.startswith('Java'):
            # Note: the signature extracted does not have return value info
            cls, method, sig = extract_names(s.name)
            func_ptr = s.rebased_addr
            if dex is None:
                # without the class info from Dex, this is it
                Record(cls, method, f'({sig})', func_ptr, s.name, None, False, True)
            else:
                # further verify and improve signature with return info
                refactored_cls_name = f'L{cls.replace(".", "/")};'
                for m in native_methods:
                    if m.get_method().get_class_name() == refactored_cls_name \
                       and m.name == method:
                        if sig is None or m.descriptor.startswith(f'({sig})'):
                            sig = m.descriptor
                            if 'static' in m.access:
                                is_static_method = True
                            else:
                                is_static_method = False
                            Record(cls, method, sig, func_ptr, s.name,
                                   is_static_method, False, True)
                            break


def extract_names(symbol):
    """Extract class and method name from exported JNI function symbol name.
    https://docs.oracle.com/javase/7/docs/technotes/guides/jni/spec/design.html
    provides the naming convention.
    NOTE: this analysis do not support Unicode characters in the name.
    """
    sig = None
    # remove prefix
    symbol = symbol.lstrip('Java_')
    if '__' in symbol:
        full_method, sig = symbol.split('__')
    else:
        full_method = symbol
    parts = re.split(r'_(?=[a-zA-Z])', full_method)
    method_name = parts[-1].replace('_1', '_')
    cls_name = '.'.join(parts[0:-1]).replace('_1', '_')
    if sig is not None:
        sig = '/'.join(re.split(r'_(?=[a-zA-Z])', sig)).replace('_2',';')\
                .replace('_3', '[')
    return cls_name, method_name, sig


def record_dynamic_jni_functions(proj, jvm_ptr, jenv_ptr, dex=None, records=None):
    state = get_prepared_jni_onload_state(proj, jvm_ptr, jenv_ptr, dex)
    tech = LengthLimiter(DYNAMIC_ANALYSIS_LENGTH)
    simgr = proj.factory.simgr(state)
    simgr.use_technique(tech)
    try:
        simgr.run()
    except Exception as e:
        logger.warning(f'Collect dynamically registered JNI function failed: {e}')
    # for multiprocess running. param "records" should be a
    # multiprocessing.Manager().dict()
    if records is not None:
        records.update(Record.RECORDS)


def jni_env_prepare_in_object(proj):
    jni_addr_size = proj.arch.bits // 8
    jvm_size = jni_addr_size * len(jvm)
    jenv_size = jni_addr_size * len(jenv)
    jvm_ptr = proj.loader.extern_object.allocate(jvm_size)
    jenv_ptr = proj.loader.extern_object.allocate(jenv_size)
    for idx, name in enumerate(jvm):
        addr = jvm_ptr + idx * jni_addr_size
        try_2_hook(name, proj, addr)
    for idx, name in enumerate(jenv):
        addr = jenv_ptr + idx * jni_addr_size
        try_2_hook(name, proj, addr)
    register_jni_relevant_data_type()
    return jvm_ptr, jenv_ptr


def try_2_hook(jni_func_name, proj, addr):
    proc = JNI_PROCEDURES.get(jni_func_name)
    if proc:
        proj.hook(addr, proc())
    else:
        proj.hook(addr, NotImplementedJNIFunction())


def jni_env_prepare_in_state(state, jvm_ptr, jenv_ptr, dex=None):
    # store JVM and JENV pointer on the state for global use
    state.globals['jvm_ptr'] = jvm_ptr
    state.globals['jni_invoke_interface'] = jvm
    state.globals['jenv_ptr'] = jenv_ptr
    state.globals['jni_native_interface'] = jenv
    if dex is not None:
        state.globals['dex'] = dex
    addr_size = state.project.arch.bits
    for idx in range(len(jvm)):
        jvm_func_addr = jvm_ptr + idx * addr_size // 8
        state.memory.store(addr=jvm_func_addr,
                           data=state.solver.BVV(jvm_func_addr, addr_size),
                           endness=state.project.arch.memory_endness)
    for idx in range(len(jenv)):
        jenv_func_addr = jenv_ptr + idx * addr_size // 8
        state.memory.store(addr=jenv_func_addr,
                           data=state.solver.BVV(jenv_func_addr, addr_size),
                           endness=state.project.arch.memory_endness)


def get_prepared_jni_onload_state(proj, jvm_ptr, jenv_ptr, dex=None):
    func_jni_onload = proj.loader.find_symbol(JNI_LOADER)
    state = proj.factory.call_state(func_jni_onload.rebased_addr, jvm_ptr)
    jni_env_prepare_in_state(state, jvm_ptr, jenv_ptr, dex)
    return state


def analyze_jni_function(func_addr, proj, jvm_ptr, jenv_ptr, dex=None, returns=None, global_refs=None):
    func_params, updates = get_jni_function_params(proj, func_addr, jenv_ptr)
    state = proj.factory.call_state(func_addr, *func_params)
    state.globals['func_ptr'] = func_addr
    if global_refs is not None:
        for k, v in global_refs.items():
            state.globals[k] = v
    for k, v in updates.items():
        state.globals[k] = v
    jni_env_prepare_in_state(state, jvm_ptr, jenv_ptr, dex)
    tech = LengthLimiter(MAX_LENGTH)
    simgr = proj.factory.simgr(state)
    simgr.use_technique(tech)
    try:
        simgr.run()
    except Exception as e:
        logger.warning(f'Analysis JNI function failed: {e}')
        traceback.print_exc()
    # for multiprocess running. param "returns" should be a
    # multiprocessing.Manager().dict()
    if returns is not None:
        invokees = Record.RECORDS.get(func_addr).get_invokees()
        if invokees is not None:
            returns.update({func_addr: invokees})


def get_jni_function_params(proj, func_addr, jenv_ptr):
    record = Record.RECORDS.get(func_addr)
    if record is None:
        raise RecordNotFoundError('Relevant JNI function record not found!')
    # for user's JNI function, the first 2 parameters are hidden from Java side
    # and the first one will always be the JNIEnv pointer.
    params = [jenv_ptr]
    # Some parameters need to be cooperated with state update, this dict will
    # be returned for this purpose.
    state_updates = dict()
    jclass = JavaClass(record.cls)
    if record.static_method:
        jclass.init = True
    ref = proj.loader.extern_object.allocate()
    # The second hidden parameter is either a jclass or jobject of the current
    # Java class where the native method lives. If it is a static method in
    # Java side, it will be a jclass otherwise a jobject.
    params.append(ref)
    state_updates.update({ref: jclass})
    # prepare for the none hidden parameters
    plist = None
    if record.signature is not None:
        plist, has_obj = parse_params_from_sig(record.signature)
    # if no object references passed via parameters, we let Angr to use default
    # setups. Since it will not affect our analysis.
    if plist is not None and has_obj:
        symbol_values = {
                'Z': BVS('boolean_value', proj.arch.bits),
                '[Z': BVS('boolean_array', proj.arch.bits),
                'B': BVS('byte_value', proj.arch.bits),
                '[B': BVS('byte_array', proj.arch.bits),
                'C': BVS('char_value', proj.arch.bits),
                '[C': BVS('char_array', proj.arch.bits),
                'S': BVS('short_value', proj.arch.bits),
                '[S': BVS('short_array', proj.arch.bits),
                'I': BVS('int_value', proj.arch.bits),
                '[I': BVS('int_array', proj.arch.bits),
                'J': BVS('long_value', proj.arch.bits),
                '[J': BVS('long_array', proj.arch.bits),
                'F': BVS('float_value', proj.arch.bits),
                '[F': BVS('float_array', proj.arch.bits),
                'D': BVS('double_value', proj.arch.bits),
                '[D': BVS('double_array', proj.arch.bits),
        }
        for p in plist:
            param = symbol_values.get(p)
            if param is None:
                cls_name = p.lstrip('[').replace('/', '.')
                jclass = None
                if cls_name == 'java.lang.Class':
                    desc = 'object of java.lang.Class passed as parameter to ' +\
                            'JNI function which makes it not possible to get ' +\
                            'the class name'
                    jclass = JavaClass(None, desc=desc)
                else:
                    jclass = JavaClass(cls_name, init=True)
                if p.startswith('['):
                    jclass.is_array = True
                param = proj.loader.extern_object.allocate()
                state_updates.update({param: jclass})
            params.append(param)
    return params, state_updates


def parse_params_from_sig(signature):
    params_pat = r'^\((?P<params>[\w\d[/;$]*)\)'
    cls_pat = r'L[\d\w/$]*;'
    has_obj = False
    plist = None
    match = re.match(params_pat, signature)
    if match is None:
        return plist, has_obj
    param_str = match.group('params')
    ms = re.findall(cls_pat, param_str)
    if len(ms) > 0:
        has_obj = True
    plist = list()
    type_array = ''
    for p in re.split(r'[L;]', param_str):
        if f'L{p};' in ms:
            plist.append(type_array + p.replace('/', '.'))
            type_array = ''
        elif '[' in p:
            if p.endswith('['):
                type_array = '['
            if len(p) == 1:
                continue
            for i, part in enumerate(p.split('[')):
                if len(part) == 0:
                    continue
                if i == 0:
                    plist += str2list(part)
                else:
                    tmp = str2list(part)
                    tmp[0] = '[' + tmp[0]
                    plist += tmp
        else:
            plist += str2list(p)
    return plist, has_obj


def str2list(s):
    l = list()
    l[:0] = s
    return l


def register_jni_relevant_data_type():
    register_types(parse_type('struct JNINativeMethod ' +\
                              '{const char* name;' +\
                              'const char* signature;' +\
                              'void* fnPtr;}'))


def print_records(fname=None):
    header = 'invoker_cls, invoker_method, invoker_signature, invoker_symbol, ' +\
             'invoker_static_export, ' +\
             'invokee_cls, invokee_method, invokee_signature, invokee_static, ' +\
             'exit_addr, invokee_desc'
    if len(Record.RECORDS) > 0:
        f = None
        if fname is None:
            f = sys.stdout
        else:
            f = open(fname, 'w')
        print(header, file=f)
        for _, r in Record.RECORDS.items():
            print(r, file=f)
        if fname is not None:
            f.close()


def clean_records():
    Record.RECORDS.clear()

