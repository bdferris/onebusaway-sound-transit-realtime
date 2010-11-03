package org.onebusaway.sound_transit.realtime.link;

import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;

public class VehicleState {

  private final BlockInstance _blockInstance;

  private final ScheduledBlockLocation _blockLocation;

  private final int _effectiveScheduleTime;

  private final EVehiclePhase _phase;

  private final long _updateTime;

  private final VehicleRecords _records;

  public VehicleState(BlockInstance blockInstance,
      ScheduledBlockLocation blockLocation, int effectiveScheduleTime,
      EVehiclePhase phase, long updateTime, VehicleRecords records) {
    _blockInstance = blockInstance;
    _blockLocation = blockLocation;
    _effectiveScheduleTime = effectiveScheduleTime;
    _phase = phase;
    _updateTime = updateTime;
    _records = records;
  }

  public BlockInstance getBlockInstance() {
    return _blockInstance;
  }

  public ScheduledBlockLocation getBlockLocation() {
    return _blockLocation;
  }

  public int getEffectiveScheduleTime() {
    return _effectiveScheduleTime;
  }

  public EVehiclePhase getPhase() {
    return _phase;
  }

  public long getUpdateTime() {
    return _updateTime;
  }

  public VehicleRecords getRecords() {
    return _records;
  }
}