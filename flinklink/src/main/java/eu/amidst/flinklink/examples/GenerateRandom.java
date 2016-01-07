package eu.amidst.flinklink.examples;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.utils.OptionParser;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import java.util.stream.IntStream;

/**
 * Created by Hanen on 09/12/15.
 */
public class GenerateRandom {

    /* Options for generating data from a randomly sampled BN */
    static int sampleSize = 1000000;
    static int numDiscVars = 11;
    static int numStates = 5;
    static int numGaussVars = 10;
    //static int numHiddenGaussVars = 0;
    //static int numStatesHiddenDiscVars = 5;



    public static int getSampleSize() {
        return sampleSize;
    }

    public static void setSampleSize(int sampleSize) {
        GenerateRandom.sampleSize = sampleSize;
    }

    public static int getNumDiscVars() {
        return numDiscVars;
    }

    public static void setNumDiscVars(int numDiscVars) {
        GenerateRandom.numDiscVars = numDiscVars;
    }

    public static int getNumStates() {
        return numStates;
    }

    public static void setNumStates(int numStates) {
        GenerateRandom.numStates = numStates;
    }

    //public static int getNumStatesHiddenDiscVars() {
    //    return numStatesHiddenDiscVars;
    //}

    //public static void setNumStatesHiddenDiscVars(int numStatesHiddenDiscVars) {
    //    GenerateRandom.numStatesHiddenDiscVars = numStatesHiddenDiscVars;
    //}

    public static int getNumGaussVars() {
        return numGaussVars;
    }

    public static void setNumGaussVars(int numGaussVars) {
        GenerateRandom.numGaussVars = numGaussVars;
    }

    //public static int getNumHiddenGaussVars() {
    //    return numHiddenGaussVars;
    //}

    //public static void setNumHiddenGaussVars(int numHiddenGaussVars) {
    //    GenerateRandom.numHiddenGaussVars = numHiddenGaussVars;
    //}

    public static String classNameID(){
        return "eu.amidst.flinklink.examples.GenerateRandom";
    }

    public static String getOption(String optionName) {
        return OptionParser.parse(classNameID(), listOptions(), optionName);
    }

    public static String listOptions(){

        return  classNameID() +",\\"+
                "-sampleSize, 1000000, Sample size of the data stream\\" +
                "-DiscV, 11, Num of discrete variables (including the class variable) \\"+
                "-numStates, 5, Num states of all discrete variables (including the class variable)\\"+
                "-GausV, 10, Num of Gaussian variables\\";
                //"-SPGausV, 2, Num of Hidden Gaussian super-parent variables\\"+
                //"-SPDiscV, 10, Num of states for Hidden Discrete super-parent variable\\";
    }

    public static int getIntOption(String optionName){
        return Integer.parseInt(getOption(optionName));
    }

    public static void loadOptions(){
        setSampleSize(getIntOption("-sampleSize"));
        setNumDiscVars(getIntOption("-DiscV"));
        setNumStates(getIntOption("-numStates"));
        setNumGaussVars(getIntOption("-GausV"));
        //setNumHiddenGaussVars(getIntOption("-SPGausV"));
        //setNumStatesHiddenDiscVars(getIntOption("-SPDiscV"));
    }

    public static void main(String[] args) throws Exception {

        OptionParser.setArgsOptions(GenerateRandom.class, args);

        GenerateRandom.loadOptions();

        /* Create DAG */
        Variables variables = new Variables();

        IntStream.range(0, getNumDiscVars() -1)
                .forEach(i -> variables.newMultionomialVariable("DiscreteVar" + i, getNumStates()));

        IntStream.range(0, getNumGaussVars())
                .forEach(i -> variables.newGaussianVariable("GaussianVar" + i));

        Variable classVar = variables.newMultionomialVariable("ClassVar", getNumStates());

        //if(getNumHiddenGaussVars() > 0)
            //IntStream.rangeClosed(0, getNumHiddenGaussVars() - 1).forEach(i -> variables.newGaussianVariable("GaussianSPVar_" + i));
        //if(numStatesHiddenDiscVars > 0)
        //Variable discreteHiddenVar = variables.newMultionomialVariable("DiscreteSPVar", getNumStatesHiddenDiscVars());

        DAG dag = new DAG(variables);

        /* Link variables */
        dag.getParentSets().stream()
                .filter(parentSet -> !parentSet.getMainVar().equals(classVar) && !parentSet.getMainVar().getName().startsWith("GaussianSPVar_")
                        && !parentSet.getMainVar().getName().startsWith("DiscreteSPVar"))
                .forEach(w -> {
                    w.addParent(classVar);
                    //w.addParent(discreteHiddenVar);
                    //if (getNumHiddenGaussVars() > 0 && w.getMainVar().isNormal())
                    //   IntStream.rangeClosed(0, getNumHiddenGaussVars() - 1).forEach(i -> w.addParent(variables.getVariableByName("GaussianSPVar_" + i)));
                });

        /*Add classVar as parent of all super-parent variables*/
        //if(getNumHiddenGaussVars() > 0)
        //    IntStream.rangeClosed(0, getNumHiddenGaussVars()-1).parallel()
        //            .forEach(hv -> dag.getParentSet(variables.getVariableByName("GaussianSPVar_" + hv)).addParent(classVar));
        //dag.getParentSet(variables.getVariableByName("DiscreteSPVar")).addParent(classVar);

        /* Create the Bayesian network */
        BayesianNetwork bn = new BayesianNetwork(dag);
        System.out.println("\n Number of variables \n " + bn.getDAG().getVariables().getNumberOfVars());
        System.out.println(dag.toString());

        /* Sample and Save the data */
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.setSeed(0);

        //The method sampleToDataStream returns a DataStream with ten DataInstance objects.
        DataStream<DataInstance> dataStream = sampler.sampleToDataStream(getSampleSize()); // Defines the size of the data

        DataStreamWriter.writeDataToFile(dataStream, "./data.arff");

    }

}