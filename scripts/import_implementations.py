import logging

from angr import SimProcedure
from angr.procedures.libc.memcpy import memcpy
from cle import SymbolType

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)

class ReturnZero(SimProcedure):
    def run(self):
        self.ret(0)

class UnimplementedHook(SimProcedure):
    def __init__(self, symbol_name, *args, **kwargs):
        super(UnimplementedHook, self).__init__(*args, **kwargs)
        self.symbol_name = symbol_name

    def run(self):
        logger.warning("Symbol '%s' called but corresponding function is NOT implemented." % self.symbol_name)
        self.ret()

# Symbol name, SimProcedure
IMPLEMENTED_IMPORTS = [
    ("pthread_mutex_lock", ReturnZero),
    ("pthread_mutex_unlock", ReturnZero),
    ("__aeabi_memcpy", memcpy)
    ]

def hookAllImportSymbols(proj):
    # Set hook on implemented imports
    for symbName, SimProc in IMPLEMENTED_IMPORTS:
        if proj.loader.find_symbol(symbName):
            proj.hook_symbol(symbName, SimProc(), replace=True)

    # Set warning SimProcedure on unimplemented imports
    for symb in proj.loader.symbols :
        if symb.is_import and symb.type == SymbolType.TYPE_FUNCTION:
            if symb.resolvedby:
                symb_addr = symb.resolvedby.rebased_addr
            else:
                symb_addr = symb.rebased_addr
            if proj.is_hooked(symb_addr):
                simProc = proj.hooked_by(symb_addr)
                if not simProc.is_stub:
                    # This symbol is already implemented by a SimProcedure
                    continue
            proj.hook_symbol(symb_addr,  UnimplementedHook(symb.name), replace=False)
