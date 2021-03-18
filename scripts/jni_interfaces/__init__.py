import importlib
# from angr import SimProcedure as SP
from .common import JNIProcedureBase as JPB

JNI_PROCEDURES = dict()


def find_simprocs(module_name, container):
    module = importlib.import_module(module_name, 'jni_interfaces')
    for attr_name in dir(module):
        attr = getattr(module, attr_name)
        if isinstance(attr, type) and issubclass(attr, JPB):
            container.update({attr_name: attr})


find_simprocs('.jni_invoke', JNI_PROCEDURES)
find_simprocs('.jni_native', JNI_PROCEDURES)



