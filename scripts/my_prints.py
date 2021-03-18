RED = "\33[91m"
GREEN = "\33[92m"
BOLD = "\033[1m"
END = "\033[0m"
BLUE = "\33[34m"
YELLOW = "\033[33m"


def pprint(prefix: str, msg: str, color: str):
    print(f"{BOLD}{color}[{prefix}] {msg}{END}")


def perror(msg: str):
    pprint("!", msg, RED)


def psuccess(msg: str):
    pprint("✓", msg, GREEN)


def pinfo(msg: str):
    pprint("*", msg, BLUE)

def pwarning(msg: str):
    pprint("*", msg, YELLOW)
