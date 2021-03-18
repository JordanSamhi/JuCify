import archinfo
from ..common import JNIProcedureBase as JPB
from ..common import JNIEnvMissingError


class GetEnv(JPB):
    def run(self, jvm, env, version):
        jenv_ptr = self.state.globals.get('jenv_ptr')
        if jenv_ptr:
            self.state.memory.store(env, jenv_ptr, endness=archinfo.Endness.LE)
        else:
            raise JNIEnvMissingError('"jenv_ptr" is not stored in state. ')
        return self.JNI_OK


