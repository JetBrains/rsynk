import argparse
import os


def get_path(dir_path, file_name, create=False, backup=False):
	file_path = "{0}/{1}".format(dir_path, file_name)
	if create:	
		if os.path.isfile(file_path) and backup:
			os.rename(file_path, "{0}.backup".format(file_path))
	
		with open(file_path, "w+"):
			pass
	return os.path.abspath(file_path)


parser = argparse.ArgumentParser(description="Create source file and print rsync comands with proper arguments" +
                                             "to transfer this file via native rsync (to play with breakpoints inside)")

parser.add_argument("--no-create", dest="no_create", action="store_const", const="no_create",
                    help="specifies whether file should be created/overriden or not")
parser.add_argument("directory", metavar="dir", help="directory where the mess'll be created")

args = parser.parse_args()

lorem_ipsum = ("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor "
               "incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud "
               "exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure "
               "dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. "
               "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit "
               "anim id est laborum.")

from_file_name = "from.txt"
to_file_name = "to.txt"
sniffed_input_name = "sniffed.input.log"
sniffed_output_name = "sniffed.output.log"

create = args.no_create is None

from_file_path = get_path(args.directory, from_file_name, create)
to_file_path = get_path(args.directory, to_file_name, create)
sniffed_input_path = get_path(args.directory, sniffed_input_name, create, backup=True)
sniffed_output_path = get_path(args.directory, sniffed_output_name, create, backup=True)

if (create):
	if args.directory.endswith("/"):
	    args.directory = args.directory[:len(args.directory) - 1]

	with open(from_file_path, "w") as rsync_input:
	    rsync_input.write(lorem_ipsum)

	print("Info: all initial fiels have been created")

print("")
print("########################################################")
print("")
print("Run rsync: ")
print("rsync --rsync-path=/usr/local/bin/rsync -v --protocol 31 -e "
      "\"ssh -p 22 -o StrictHostKeyChecking=no\" "
      "localhost:{0} {1}".format(from_file_path, to_file_path))

print("")
print("That makes '{0}' content be transerred to file '{1}'".format(from_file_name, to_file_name))
print("")

print("For negotiating with rsync manually run:")
print("rsync --server --sender -ve.LsfxC . "
      "{0} < {1} > {2}".format(from_file_path, sniffed_input_path, sniffed_output_path))

print("")
print("Provide input by writing it at '{0}'".format(sniffed_input_name))
print("Read output from '{0}'".format(sniffed_output_name))
print("")
print("(all files are located at '{0})".format(os.path.abspath(args.directory)))
print("")
print("########################################################")
print("")


