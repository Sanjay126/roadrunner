//************************************************************
// Copyright 2019 ANI Technologies Pvt. Ltd.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//************************************************************

package com.olacabs.roadrunner.impl.filter;

import java.util.ArrayList;
import java.util.List;

import com.olacabs.BitSetExposed;
import com.olacabs.roadrunner.api.filter.ColumnFilterLong;
import com.olacabs.roadrunner.api.filter.DictionaryLong;
import com.olacabs.roadrunner.api.RecordIndexes;

import com.olacabs.roadrunner.impl.BitsetCache;
import com.olacabs.roadrunner.impl.RecordIndexesImpl;
import com.olacabs.roadrunner.impl.MapWrapper;
import com.olacabs.roadrunner.impl.PartitionBucket;
import com.olacabs.roadrunner.utils.RoadRunnerUtils;

/**
 * Don't Edit this code, it is generated using templates
 */
public class ColumnFilterLongRepeatedImpl implements ColumnFilterLong {

	String fieldName;
	MapWrapper[] invertedIndexArray;
	DictionaryLong dictionary;

	BitSetExposed[] availablePosBitSets;
	BitsetCache bitsCache;

	public ColumnFilterLongRepeatedImpl(String fieldName, MapWrapper[] invertedIndexArray,
			DictionaryLong dictionary, BitSetExposed[] availablePosBitSets, BitsetCache bitsCache) {
		super();
		this.fieldName = fieldName;
		this.invertedIndexArray = invertedIndexArray;
		this.dictionary = dictionary;
		this.availablePosBitSets = availablePosBitSets;
		this.bitsCache = bitsCache;
	}

	private final long[] getKeysForEqualToOp(final long value) {
		if(dictionary.contains(value)) {
			return new long[]{value};
		}
		return new long[]{};
	} 

	private final long[] getKeysForEqualToOp(final long[] values) {
		List<Long> dataList = new ArrayList<>();
		for(int i = 0; i < values.length; i++) {
			if(dictionary.contains(values[i])) {
				dataList.add(values[i]);
			}
		}
		if(dataList.isEmpty()) {
			return new long[]{};
		}

		int size = dataList.size();
		long[] keys = new long[size];
		for(int i = 0; i < size; i++ ) {
			keys[i] = dataList.get(i);
		}
		return keys;
	} 

	public final RecordIndexes opwork(final Operation operation, final long... values) {
		long[] valuesToCheck;
		switch (operation) {
		case GREATER_THAN:
			valuesToCheck = dictionary.greaterThan(values[0]);
			break;

		case GREATER_THAN_EQUAL_TO:
			valuesToCheck = dictionary.greaterThanEqualTo(values[0]); 
			break;

		case LESSER_THAN:
			valuesToCheck = dictionary.lessThan(values[0]);
			break;

		case LESSER_THAN_EQUAL_TO:
			valuesToCheck = dictionary.lessThanEqualTo(values[0]);
			break;

		case EQUAL_TO:
			if(values.length == 1) {
				valuesToCheck = getKeysForEqualToOp(values[0]);
			} else {
				valuesToCheck = getKeysForEqualToOp(values);
			}
			break;

		case RANGE:
			valuesToCheck = dictionary.rangeValues(values[0], values[1]);
			break;
			
		case MISSING:
			valuesToCheck = dictionary.keys();
			break;
		
		case EXISTS:
			valuesToCheck = dictionary.keys();
			break;

		default:
			valuesToCheck = new long[]{};
			break;
		}
		
		int length = availablePosBitSets.length;
		int valuesLength = valuesToCheck.length;
		String keyPrefix = fieldName + PartitionBucket.SEPARATOR;
		
		BitSetExposed[] intermediateBits = new BitSetExposed[length];
		BitSetExposed valBitSet = this.bitsCache.take();
		
		for ( int i=0; i < length; i++) {
			intermediateBits[i] = this.bitsCache.take();
			BitSetExposed bs = availablePosBitSets[i];

			if(bs.nextSetBit(0) >= 0) {
				intermediateBits[i].or(bs);
				for(int pos = 0; pos < valuesLength; pos++) {
					String key = keyPrefix + String.valueOf(valuesToCheck[pos]);
					BitSetExposed bitSet = invertedIndexArray[i].dataMap.get(key);
					if(bitSet != null) valBitSet.or(bitSet);
				}
				if(operation == Operation.MISSING) {
					intermediateBits[i].andNot(valBitSet);
				} else {
					intermediateBits[i].and(valBitSet);
				}
			}
			valBitSet.clear();	
		}
		return new RecordIndexesImpl(intermediateBits, availablePosBitSets, bitsCache);
	}

	@Override
	public final RecordIndexes greaterThan(long value) {
		RoadRunnerUtils.validateCompareValues(fieldName, value);
		return opwork(Operation.GREATER_THAN, value);
	}

	@Override
	public final RecordIndexes lessThan(long value) {
		RoadRunnerUtils.validateCompareValues(fieldName, value);
		return opwork(Operation.LESSER_THAN, value);
	}

	@Override
	public final RecordIndexes greaterThanEqualTo(long value) {
		RoadRunnerUtils.validateCompareValues(fieldName, value);
		return opwork(Operation.GREATER_THAN_EQUAL_TO, value);
	}

	@Override
	public final RecordIndexes lessThanEqualTo(long value) {
		RoadRunnerUtils.validateCompareValues(fieldName, value);
		return opwork(Operation.LESSER_THAN_EQUAL_TO, value);
	}

	@Override
	public final RecordIndexes equalTo(long value) {
		RoadRunnerUtils.validateCompareValues(fieldName, value);
		return opwork(Operation.EQUAL_TO, value);
	}

	@Override
	public final RecordIndexes equalTo(long[] values) {
		RoadRunnerUtils.validateCompareValues(fieldName, values);
		return opwork(Operation.EQUAL_TO, values);
	}

	@Override
	public final RecordIndexes range(long minValue, long maxValue) {
		long[] values = new long[]{minValue, maxValue};
		RoadRunnerUtils.validateCompareValues(fieldName, values);
		return opwork(Operation.RANGE, values);
	}
	
	@Override
	public final RecordIndexes missing() {
		return opwork(Operation.MISSING, null);
	}
	
	@Override
	public final RecordIndexes exists() {
		return opwork(Operation.EXISTS, null);
	}
	
}

