package org.onebusaway.sound_transit.realtime.link;

import java.util.ArrayList;
import java.util.List;

import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;

public class VehicleRecords {

  private List<Record> _records = new ArrayList<Record>();

  private List<BlockStopTimeEntry> _blockStopTimes = new ArrayList<BlockStopTimeEntry>();

  public VehicleRecords(Record record, BlockStopTimeEntry blockStopTime) {
    addRecord(record, blockStopTime);
  }

  public void addRecord(Record record, BlockStopTimeEntry blockStopTime) {
    _records.add(record);
    _blockStopTimes.add(blockStopTime);
  }
  
  public List<Record> getRecords() {
    return _records;
  }
  
  public Record getFirstRecord() {
    return _records.get(0);
  }

  public Record getLastRecord() {
    return _records.get(_records.size() - 1);
  }
  
  public BlockStopTimeEntry getFirstBlockStopTime() {
    return _blockStopTimes.get(0);
  }

  public BlockStopTimeEntry getLastBlockStopTime() {
    return _blockStopTimes.get(_blockStopTimes.size() - 1);
  }
}
