import sys
import os
import networkx as nx
from my_prints import *
import argparse


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("-d", "--dot", help="The path of the "\
                        "dot file to process", type=str, required=True)
    parser.add_argument("-e", "--entrypoints", help="The "\
                        "path of the entrypoints file", type=str, required=True)
    parser.add_argument("-w", "--write", help="Write "\
                        "new graph into specified location", type=str, required=False)
    args = parser.parse_args()
    return args


def load_entrypoints(location: str) -> list:
    with open(location, 'r') as f:
        for line in f.readlines():
            yield line.rstrip()


def populate_new_G(node: str, graph, new_graph):
    if graph.has_node(node):
        succ = graph.successors(node)
        if not new_G.has_node(node):
            new_G.add_node(node)
        for s in succ:
            new_G.add_edge(node, s)
            populate_new_G(s, graph, new_graph)


def check_files_existence(*files):
    for f in files:
        if not os.path.exists(f):
            perror(f"File {f} does not exist")
            perror("Exiting program.")
            sys.exit(1)


if __name__ == "__main__":
    args = parse_args()
    entrypoints_file = args.entrypoints
    dot_file = args.dot
    entrypoints = entrypoints_file
    check_files_existence(dot_file, entrypoints)
    entrypoints = list(load_entrypoints(entrypoints_file))
    G = nx.drawing.nx_agraph.read_dot(dot_file)
    new_G = nx.DiGraph()
    for e in entrypoints:
        node = f"Node_{e}"
        populate_new_G(node, G, new_G)
    if args.write:
        nx.drawing.nx_agraph.write_dot(new_G, args.write)
