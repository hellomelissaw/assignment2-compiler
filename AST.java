import java.util.ArrayList;
import java.util.List;

enum SignalType {
    INPUT, LATCH_OUTPUT, UPDATE_OUTPUT
}

public abstract class AST {
    public void error(String msg) {
        System.err.println(msg);
        System.exit(-1);
    }
}

abstract class Program extends AST {
    abstract public Boolean eval(Environment env);

}

/*class Sequence extends Program {
    List<Program> ps;
    Sequence(List<Program> ps) {this.ps = ps;}
    public Boolean eval(Environment env) {
        for(Program p : ps) p.eval(env);
    }
}*/

/* Expressions are similar to arithmetic expressions in the impl
   language: the atomic expressions are just Signal (similar to
   variables in expressions) and they can be composed to larger
   expressions with And (Conjunction), Or (Disjunction), and
   Not (Negation) */

abstract class Expr extends AST {
    abstract public Boolean eval(Environment env);

    abstract public Boolean check(Environment env);

    //abstract public SignalType typecheck(Environment env);

    //TODO implement a settype and typcheck expressions, that they either are output of  a prev update
    //or an input signal or output latch
}

class Conjunction extends Expr {
    Expr e1, e2;

    Conjunction(Expr e1, Expr e2) {
        this.e1 = e1;
        this.e2 = e2;
    }

    public Boolean eval(Environment env) {
        return e1.eval(env) && e2.eval(env);
    }

    public Boolean check(Environment env) {
        return e1.check(env) && e2.check(env);
    }

}

class Disjunction extends Expr {
    Expr e1, e2;

    Disjunction(Expr e1, Expr e2) {
        this.e1 = e1;
        this.e2 = e2;
    }

    public Boolean eval(Environment env) {
        return e1.eval(env) || e2.eval(env);
    }

    public Boolean check(Environment env) {
        return e1.check(env) && e2.check(env);
    }

}

class Negation extends Expr {
    Expr e;

    Negation(Expr e) {
        this.e = e;
    }

    public Boolean eval(Environment env) {
        return !e.eval(env);
    }
    public Boolean check(Environment env) {
        return e.check(env);
    }

}

class Signal extends Expr {
    String varname; // a signal is just identified by a name

    Signal(String varname) {
        this.varname = varname;
    }

    public Boolean eval(Environment env) {
        return env.getVariable(varname);
    }

    public Boolean check(Environment env) {
        if (env.typecheck(varname) != null){
            System.out.println("printout the varname "+varname + " returning true");
            return true;
        } else {
            System.out.println("printout the varname "+varname + " returning false");
            return false;
        }
    }
}

// Latches have an input and output signal

class Latch extends AST {
    String inputname;
    String outputname;

    Latch(String inputname, String outputname) {
        this.inputname = inputname;
        this.outputname = outputname;
    }

    //TODO we should make a generic typechecker, that just returns the type and then use it in here. SO gør intialize om
    //so we should check the latchoutput , is not already in the inputs
    public void initialize(Environment env) {
        System.out.println("input name in initialize latch: " + inputname);
        //SignalType t1 = env.typecheck(inputname);
        SignalType t2 = env.typecheck(outputname);
        //System.out.println("input name: " + inputname + "type: " + t1);
        System.out.println("output name: " + outputname + "type: " + t2);
        if (t2 == null) {
            env.setVariable(outputname, false);
        } else {
            System.out.println("unexpected Latch signal");
            System.exit(-1);
        }
    }

    public void nextCycle(Environment env) {
        System.out.println("In next cycle latch inputname: " + inputname);
        env.setVariable(outputname, env.getVariable(inputname));
    }

}

// An Update is any of the lines " signal = expression "
// in the .update section
//TODO check if the update input is not any of input,latchout,or prev update , typecheck the output of update for the mentioed 3
class Update extends AST {
    String name;
    Expr e;

    Update(String name, Expr e) {
        this.e = e;
        this.name = name;

    }

    public void eval(Environment env) {
        env.setVariable(name, e.eval(env));
    }

    public Boolean typecheck(Environment env){
        if (e.check(env) && env.typecheck(name)==null) {
            env.setSignalType(name,SignalType.UPDATE_OUTPUT);
            return true;
        }
        else return false;
    }

    public Boolean updateIsValid(Update update, Environment env) {
        SignalType t = env.typecheck(update.name);
        if (t == null) {
            return true;
        } else {
            return false;
        }
    }
}

/* A Trace is a signal and an array of Booleans, for instance each
   line of the .simulate section that specifies the traces for the
   input signals of the circuit. It is suggested to use this class
   also for the output signals of the circuit in the second
   assignment.
*/

class Trace extends AST {
    String signal;
    Boolean[] values;

    Trace(String signal, Boolean[] values) {
        this.signal = signal;
        this.values = values;
    }

    public String toString() {
        String signalTrace = "";
        for (boolean value : values) {
            if (value)
                signalTrace += "1";
            else
                signalTrace += "0";

        }
        return signalTrace;
    }
}

/* The main data structure of this simulator: the entire circuit with
   its inputs, outputs, latches, and updates. Additionally for each
   input signal, it has a Trace as simulation input. 
   
   There are two variables that are not part of the abstract syntax
   and thus not initialized by the constructor (so far): simoutputs
   and simlength. It is suggested to use them for assignment 2 to
   implement the interpreter:
 
   1. to have simlength as the length of the traces in siminputs. (The
   simulator should check they have all the same length and stop with
   an error otherwise.) Now simlength is the number of simulation
   cycles the interpreter should run.

   2. to store in simoutputs the value of the output signals in each
   simulation cycle, so they can be displayed at the end. These traces
   should also finally have the length simlength.
*/

class Circuit extends AST {
    String name;
    List<String> inputs;
    List<String> outputs;
    List<Latch> latches;
    List<Update> updates;
    List<Trace> siminputs;
    List<Trace> simoutputs;
    int simlength;

    Circuit(String name,
            List<String> inputs,
            List<String> outputs,
            List<Latch> latches,
            List<Update> updates,
            List<Trace> siminputs) {
        this.name = name;
        this.inputs = inputs;
        this.outputs = outputs;
        this.latches = latches;
        this.updates = updates;
        this.siminputs = siminputs;
        simlength = siminputs.get(0).values.length;
        simoutputs = new ArrayList<Trace>();
    }

    public void initialize(Environment env) {
        for (Trace trace : siminputs) {
            if (simlength == 0) {
                System.err.println("Siminput value array length 0.");
                System.exit(-1);
            }
            env.setVariable(trace.signal, trace.values[0]);

            if (env.typecheck(trace.signal) == null) {
                env.setSignalType(trace.signal, SignalType.INPUT);
            } else {
                System.out.println("input duplicated.");
                System.exit(-1);
            }


        }

        for (String output : outputs) {
            Boolean[] values = new Boolean[simlength];
            simoutputs.add(new Trace(output, values));

        }

        for (Latch latch : latches) {
            latch.initialize(env);
            env.setSignalType(latch.outputname, SignalType.LATCH_OUTPUT);

        }

        for (Update update : updates) {
            update.typecheck(env);
            update.eval(env);

        }
        for (Trace trace : simoutputs) {
            System.out.println("Trace simoutputs name: " + trace.signal);
            trace.values[0] = env.getVariable(trace.signal);
        }


        //System.out.println("Printing the init environment: \n " + env.toString() + "\n\n");
    }

    public void nextCycle(Environment env, int i) {
        for (Trace trace : siminputs) {
            if (trace.values.length == 0) {
                System.err.println("Siminput value array length 0.");
                System.exit(-1);
            }
            env.setVariable(trace.signal, trace.values[i]);

        }

        for (Latch latch : latches) {
            latch.nextCycle(env);

        }

        for (Update update : updates) {
            update.eval(env);

        }

        for (Trace trace : simoutputs) {
            System.out.println("trace simoutputs in circuit net cycle: " + trace.signal);
            trace.values[i] = env.getVariable(trace.signal);
        }

        //System.out.println("Printing env for cycle " + i + ": \n " + env.toString() + "\n\n");
    }

    public void runSimulator(Environment env) {
        initialize(env);

        for (int i = 1; i < simlength; i++) {
            nextCycle(env, i);
        }

        for (Trace trace : siminputs)
            System.out.println(trace.toString() + " " + trace.signal);

        for (Trace trace : simoutputs)
            System.out.println(trace.toString() + " " + trace.signal);
    }
}
