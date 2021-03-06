package co.nubetech.crux.server;

import java.util.Arrays;
import java.util.List;

import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.log4j.Logger;

import co.nubetech.crux.model.Alias;
import co.nubetech.crux.model.ColumnAlias;
import co.nubetech.crux.model.Report;
import co.nubetech.crux.model.ReportDesign;
import co.nubetech.crux.model.RowAlias;
import co.nubetech.crux.server.aggregate.GroupingAggregationImpl;

public class ServerUtil {
	
	private final static Logger logger = Logger.getLogger(ServerUtil.class);
	
	/**
	 * This is not the most optimized piece of code
	 * we should go to the byte buffers
	 * lets clean this later once the functionality works end to end
	 * @param results
	 * @param alias
	 * @return
	 */
	public static byte[] getValue(List<KeyValue> results, Alias alias) {
		byte[] value = null;
		if (alias instanceof RowAlias) {
			RowAlias rowAlias = (RowAlias) alias;
			value = results.get(0).getRow();	
			int offset = rowAlias.getOffset();
			int length = rowAlias.getOffset() + rowAlias.getLength();
			logger.debug("Truncating row between index " + offset
					+ " and " + length);
			value = Arrays.copyOfRange(value, offset, length);
		}
		else {
			ColumnAlias colAlias = (ColumnAlias) alias;
			String family = colAlias.getColumnFamily();
			String qualifier = colAlias.getQualifier();
			logger.debug("Checking for family and qualifier "  + family + ", " + qualifier);
			byte[] familyBytes = family.getBytes();
			byte[] qualifierBytes = qualifier.getBytes();
			for (KeyValue kv: results) {
				logger.debug("Checking in KV " + kv.toString());
				if (Bytes.compareTo(kv.getFamily(), familyBytes) == 0) {
					if (Bytes.compareTo(qualifierBytes, kv.getQualifier()) == 0) {
						logger.debug("Yes I found the value for cf:q");
						value = kv.getValue();
						break;
					}
				}
			}
		}
		return value;
	}
	
	public static byte[] getValue(Result result, Alias alias) {
		byte[] value = null;
		if (alias instanceof RowAlias) {
			RowAlias rowAlias = (RowAlias) alias;
			value = result.getRow();
			value = Arrays.copyOfRange(value, rowAlias.getOffset(), rowAlias.getOffset() + rowAlias.getLength());
		}
		else {
			ColumnAlias colAlias = (ColumnAlias) alias;
			String family = colAlias.getColumnFamily();
			String qualifier = colAlias.getQualifier();
			value = result.getColumnLatest(family.getBytes(), qualifier.getBytes()).getValue();
		}
		return value;
	}
	
	public static Object getValue(byte[] b, Alias alias) {
		logger.debug("Alias " + alias + " , and value type is " + alias.getValueType() + 
				" and bytes are " + b );
		if (b != null) {
			logger.debug("bytes.length is " + b.length);
		}
		Object returnObject = alias.getValueType().fromBytes(b);
		logger.debug("Returning " + returnObject);
		return returnObject;
	}
	
	public static Object getObjectValue(Result result, Alias alias) {
		return getValue(getValue(result, alias), alias);
	}
	
	public static Alias getAlias(ReportDesign design) {
		Alias alias = design.getRowAlias();
		logger.debug("The ReportDesign " + design + " has the row alias " + alias);
		 if (alias == null) {
			 alias = design.getColumnAlias(); 
			 logger.debug("The ReportDesign " + design + " has the col alias " + alias);
		 }
		 return alias;
	}	

}
