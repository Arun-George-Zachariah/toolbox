package eu.amidst.moalink.arffWekaReader;

import eu.amidst.corestatic.datastream.Attribute;
import eu.amidst.corestatic.datastream.filereaders.DataRow;
import weka.core.Instance;

/**
 * Created by ana@cs.aau.dk on 14/11/14.
 */
public class DataRowWeka implements DataRow {

    private Instance dataRow;

    public DataRowWeka(Instance dataRow){
        this.dataRow = dataRow;
    }
    @Override
    public double getValue(Attribute att) {
        return dataRow.value(att.getIndex());
    }

    @Override
    public void setValue(Attribute att, double value) {
        dataRow.setValue(att.getIndex(), value);
    }
}
