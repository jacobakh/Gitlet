package gitlet;

import java.io.IOException;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author
 */
public class Main {
    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        if (args.length == 0) {
            System.err.println("Need a subcommand");
            return;
        }
        // first load gitlet object

        try {
            Gitlet gitlet = new Gitlet();
            switch (args[0]) {
                case "init":
                    gitlet.init();
                    break;
                case "add":
                    gitlet.add(args[1]);
                    break;
                case "commit":
                    if (args[1].equals("")) {
                        System.err.println("Please enter a commit message.");
                        return;
                    }
                    gitlet.commit(args[1]);
                    break;
                case "rm":
                    gitlet.rm(args[1]);
                    break;
                case "log":
                    gitlet.log();
                    break;
                case "global-log":
                    gitlet.globallog();
                    break;
                case "find":
                    gitlet.find(args[1]);
                    break;
                case "status":
                    gitlet.status();
                    break;
                case "checkout":
                    gitlet.checkout(args);
                    break;
                case "branch":
                    gitlet.branch(args[1]);
                    break;
                case "rmbranch":
                    gitlet.rmbranch(args[1]);
                    break;
                case "reset":
                    gitlet.reset(args[1]);
                    break;
                case "merge":
                    gitlet.merge(args[1]);
                    break;
                default:
            }
            gitlet.saveState();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

}
