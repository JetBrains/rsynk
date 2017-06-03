import argparse
import os

def create_file(dir_path, file_name):
	with open("{0}/{1}".format(dir_path, file_name), "w+"):
		pass
	return os.path.abspath("{0}/{1}".format(dir_path, file_name))

parser = argparse.ArgumentParser(description="Create source file and print rsync comands with proper arguments" +
                                             "to transfer this file via native rsync (to play with breakpoints inside)")
parser.add_argument("directory", metavar="dir", help="directory where the mess'll be created")
args = parser.parse_args()


lorem_ipsum = ("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor "
				"incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud "
			  	"exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure "
			  	"dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. "
			  	"Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit "
			  	"anim id est laborum.")


if args.directory.endswith("/"):
	args.directory = args.directory[:len(args.directory) - 1]

from_file_name = "from.txt"
from_file_path = create_file(args.directory, from_file_name)

to_file_name = "to.txt"
to_file_path = create_file(args.directory, to_file_name)

sniffed_input_name = "sniffed.input"
sniffed_input_path = create_file(args.directory, sniffed_input_name)

sniffed_output_name = "sniffed.output"
sniffed_output_path = create_file(args.directory, sniffed_output_name)

with open(from_file_path, "w") as rsync_input:
	rsync_input.write(lorem_ipsum)
 
print("Run rsync: rsync --rsync-path=/usr/local/bin/rsync -v --protocol 31 -e "
	  "\"ssh -p 22 -o StrictHostKeyChecking=no\""
	  "localhost:{0} {1}/output.txt".format(from_file_path, to_file_path))

print("")
print("That makes '{0}' content be transerred to file '{1}'".format(from_file_name, to_file_name))
print("")

print("For negotiating with rsync manually run: rsync --server --sender -ve.LsfxC . "
	  "{0} < {1} > {2}".format(from_file_path, sniffed_input_path, sniffed_output_path))

print("")
print("Provide input by writing it at '{0}'".format(sniffed_input_name))
print("Read output from '{0}'".format(sniffed_output_name))
print("(all files (should be) located at '{0})".format(os.path.abspath(args.directory)))
print("")


