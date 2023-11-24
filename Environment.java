import java.util.HashMap;
import java.util.Map.Entry;

class Environment {
    private HashMap<String, Boolean> variableValues = new HashMap<String, Boolean>();
    private HashMap<String, SignalType> signalType = new HashMap<String, SignalType>();

    public Environment() {
    }
    public SignalType typecheck(String signalName) {
        for (String key :signalType.keySet()) {
            if(key.equals(signalName))
                return signalType.get(key);
        }
        //System.out.println("key not found");
        return null;
    }
    public void setVariable(String name, Boolean value) {
        variableValues.put(name, value);
    }

    public void setSignalType(String name, SignalType type) {
        signalType.put(name, type);
    }

    public Boolean getVariable(String name) {
        Boolean value = variableValues.get(name);
        if (value == null) {
            System.err.println("Variable not defined: " + name);
            System.exit(-1);
        }
        return value;
    }

    public Boolean hasVariable(String name) {
        Boolean v = variableValues.get(name);
        return (v != null);
    }

    public String toString() {
        String table = "";
        for (Entry<String, Boolean> entry : variableValues.entrySet()) {
            table += entry.getKey() + "\t-> " + entry.getValue() + "\n";
        }
        return table;
    }
}

