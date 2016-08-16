/* Generated by Streams Studio: August 15, 2016 at 8:07:04 AM EDT */
package dps.test.java;


import org.apache.log4j.Logger;

import com.ibm.streams.operator.AbstractOperator;
import com.ibm.streams.operator.OperatorContext;
import com.ibm.streams.operator.OutputTuple;
import com.ibm.streams.operator.StreamingData.Punctuation;
import com.ibm.streams.operator.StreamingInput;
import com.ibm.streams.operator.StreamingOutput;
import com.ibm.streams.operator.Tuple;
import com.ibm.streams.operator.model.InputPortSet;
import com.ibm.streams.operator.model.InputPortSet.WindowMode;
import com.ibm.streams.operator.model.InputPortSet.WindowPunctuationInputMode;
import com.ibm.streams.operator.model.InputPorts;
import com.ibm.streams.operator.model.Libraries;
import com.ibm.streams.operator.model.OutputPortSet;
import com.ibm.streams.operator.model.OutputPortSet.WindowPunctuationOutputMode;
import com.ibm.streams.operator.types.RString;
import com.ibm.streams.operator.model.OutputPorts;
import com.ibm.streams.operator.model.Parameter;
import com.ibm.streams.operator.model.PrimitiveOperator;
import com.ibm.streamsx.dps.DistributedStores;
import com.ibm.streamsx.dps.Store;
import com.ibm.streamsx.dps.StoreException;
import com.ibm.streamsx.dps.StoreFactory;
import com.ibm.streamsx.dps.StoreFactoryException;
import com.ibm.streamsx.dps.impl.DpsHelper;

/**
 * Class for an operator that receives a tuple and then optionally submits a tuple. 
 * This pattern supports one or more input streams and one or more output streams. 
 * <P>
 * The following event methods from the Operator interface can be called:
 * </p>
 * <ul>
 * <li><code>initialize()</code> to perform operator initialization</li>
 * <li>allPortsReady() notification indicates the operator's ports are ready to process and submit tuples</li> 
 * <li>process() handles a tuple arriving on an input port 
 * <li>processPuncuation() handles a punctuation mark arriving on an input port 
 * <li>shutdown() to shutdown the operator. A shutdown request may occur at any time, 
 * such as a request to stop a PE or cancel a job. 
 * Thus the shutdown() may occur while the operator is processing tuples, punctuation marks, 
 * or even during port ready notification.</li>
 * </ul>
 * <p>With the exception of operator initialization, all the other events may occur concurrently with each other, 
 * which lead to these methods being called concurrently by different threads.</p> 
 */
@PrimitiveOperator(name="JavaDPSTestOp", namespace="dps.test.java",
description="Java Operator JavaDPSTestOp")
@InputPorts({@InputPortSet(description="Port that ingests tuples", cardinality=1, optional=false, windowingMode=WindowMode.NonWindowed, windowPunctuationInputMode=WindowPunctuationInputMode.Oblivious), @InputPortSet(description="Optional input ports", optional=true, windowingMode=WindowMode.NonWindowed, windowPunctuationInputMode=WindowPunctuationInputMode.Oblivious)})
@OutputPorts({@OutputPortSet(description="Port that produces tuples", cardinality=1, optional=false, windowPunctuationOutputMode=WindowPunctuationOutputMode.Generating), @OutputPortSet(description="Optional output ports", optional=true, windowPunctuationOutputMode=WindowPunctuationOutputMode.Generating)})
@Libraries("../../../com.ibm.streamsx.dps/impl/java/lib/*")
public class JavaDPSTestOp extends AbstractOperator {
	
	private String dpsConfigFile;
	
	@Parameter(name="dpsConfigFile", optional=true)
	public void setDpsConfigFile(String dpsConfigFile) {
		this.dpsConfigFile = dpsConfigFile;
	}
	
    /**
     * Initialize this operator. Called once before any tuples are processed.
     * @param context OperatorContext for this operator.
     * @throws Exception Operator failure, will cause the enclosing PE to terminate.
     */
	@Override
	public synchronized void initialize(OperatorContext context)
			throws Exception {
    	// Must call super.initialize(context) to correctly setup an operator.
		super.initialize(context);
        Logger.getLogger(this.getClass()).trace("Operator " + context.getName() + " initializing in PE: " + context.getPE().getPEId() + " in Job: " + context.getPE().getJobId() );
        
        if(dpsConfigFile != null && !dpsConfigFile.isEmpty()) {
            DpsHelper helper = new DpsHelper();
            helper.dpsSetConfigFile(dpsConfigFile);	
        }
	}

    /**
     * Notification that initialization is complete and all input and output ports 
     * are connected and ready to receive and submit tuples.
     * @throws Exception Operator failure, will cause the enclosing PE to terminate.
     */
    @Override
    public synchronized void allPortsReady() throws Exception {
    	// This method is commonly used by source operators. 
    	// Operators that process incoming tuples generally do not need this notification. 
        OperatorContext context = getOperatorContext();
        Logger.getLogger(this.getClass()).trace("Operator " + context.getName() + " all ports are ready in PE: " + context.getPE().getPEId() + " in Job: " + context.getPE().getJobId() );
    }

    /**
     * Process an incoming tuple that arrived on the specified port.
     * <P>
     * Copy the incoming tuple to a new output tuple and submit to the output port. 
     * </P>
     * @param inputStream Port the tuple is arriving on.
     * @param tuple Object representing the incoming tuple.
     * @throws Exception Operator failure, will cause the enclosing PE to terminate.
     */
    @Override
    public final void process(StreamingInput<Tuple> inputStream, Tuple tuple)
            throws Exception {
    	
    	// Create a new tuple for output port 0
        StreamingOutput<OutputTuple> outStream = getOutput(0);
        OutputTuple outTuple = outStream.newTuple();

        // Copy across all matching attributes.
        outTuple.assign(tuple);

        Store store = null;
        StoreFactory sf = null;
        try {
			// create the store
			sf = DistributedStores.getStoreFactory();
			store = sf.createOrGetStore("Java Test Store", "rstring", "int32");
		} catch (StoreFactoryException e) {
			e.printStackTrace();
			outTuple.setString(0, "Error creating store 'Java Test Store': rc=" + e.getErrorCode() + ", msg=" + e.getErrorMessage());
	    	outStream.submit(outTuple);
			return;
		}
        
        try { 
        	// put the value
        	store.put(new RString("MyKey"), 42);
        } catch (StoreException e) {
        	e.printStackTrace();
			outTuple.setString(0, "Error using store.put(): rc=" + e.getErrorCode() + ", msg=" + e.getErrorMessage());
	    	outStream.submit(outTuple);
			return;
        }
        
        try {
            // retrieve the value
	        Object object = store.get(new RString("MyKey"));
	        if(object instanceof Integer) {
	        	Integer value = (Integer)object;
	        	outTuple.setString(0, "v=" + value);
	        }
		} catch (StoreException e) {
			e.printStackTrace();
			outTuple.setString(0, "Error using store.get(): rc=" + e.getErrorCode() + ", msg=" + e.getErrorMessage());
	    	outStream.submit(outTuple);
			return;
		}
        
        // remove the store
        try {
			sf.removeStore(store);
		} catch (StoreFactoryException e) {
			e.printStackTrace();
			outTuple.setString(0, "Error removing store: rc=" + e.getErrorCode() + ", msg=" + e.getErrorMessage());
	    	outStream.submit(outTuple);
			return;
		}
        
    	outStream.submit(outTuple);
    }
    
    /**
     * Process an incoming punctuation that arrived on the specified port.
     * @param stream Port the punctuation is arriving on.
     * @param mark The punctuation mark
     * @throws Exception Operator failure, will cause the enclosing PE to terminate.
     */
    @Override
    public void processPunctuation(StreamingInput<Tuple> stream,
    		Punctuation mark) throws Exception {
    	// For window markers, punctuate all output ports 
    	super.processPunctuation(stream, mark);
    }

    /**
     * Shutdown this operator.
     * @throws Exception Operator failure, will cause the enclosing PE to terminate.
     */
    public synchronized void shutdown() throws Exception {
        OperatorContext context = getOperatorContext();
        Logger.getLogger(this.getClass()).trace("Operator " + context.getName() + " shutting down in PE: " + context.getPE().getPEId() + " in Job: " + context.getPE().getJobId() );
        
        // TODO: If needed, close connections or release resources related to any external system or data store.

        // Must call super.shutdown()
        super.shutdown();
    }
}