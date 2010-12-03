package org.onebusaway.sound_transit.realtime.link;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.onebusaway.collections.MappingLibrary;
import org.onebusaway.collections.Min;
import org.onebusaway.gtfs.csv.CsvEntityReader;
import org.onebusaway.gtfs.csv.EntityHandler;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.realtime.api.TimepointPredictionRecord;
import org.onebusaway.realtime.api.VehicleLocationListener;
import org.onebusaway.realtime.api.VehicleLocationRecord;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocationService;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.FrequencyEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class LinkRealtimeService {

  private static Logger _log = LoggerFactory.getLogger(LinkRealtimeService.class);

  private static SimpleDateFormat _format = new SimpleDateFormat("HH:mm:ss");

  private VehicleLocationListener _vehicleLocationListener;

  private BlockCalendarService _blockCalendarService;

  private ScheduledBlockLocationService _scheduledBlockLocationService;

  private ScheduledExecutorService _executor;

  private int _refreshInterval = 60;

  private String _url;

  private String _username;

  private String _password;

  private boolean _useNtlm = false;

  private String _domain;

  private String _agencyId = "40";

  private String _stopAgencyId = "1";

  private String _vehicleIdPrefix = "link_";

  @Autowired
  public void setVehicleLocationListener(
      VehicleLocationListener vehicleLocationListener) {
    _vehicleLocationListener = vehicleLocationListener;
  }

  @Autowired
  public void setBlockCalendarService(BlockCalendarService blockCalendarService) {
    _blockCalendarService = blockCalendarService;
  }

  @Autowired
  public void setScheduledBlockLocationService(
      ScheduledBlockLocationService scheduledBlockLocationService) {
    _scheduledBlockLocationService = scheduledBlockLocationService;
  }

  public void setUrl(String url) {
    _url = url;
  }

  public void setUsername(String username) {
    _username = username;
  }

  public void setPassword(String password) {
    _password = password;
  }

  public void setDomain(String domain) {
    _domain = domain;
  }

  public void setUseNtlm(boolean useNtlm) {
    _useNtlm = useNtlm;
  }

  public void setAgencyId(String agencyId) {
    _agencyId = agencyId;
  }

  public void setStopAgencyId(String stopAgencyId) {
    _stopAgencyId = stopAgencyId;
  }

  public void setVehicleIdPrefix(String vehicleIdPrefix) {
    _vehicleIdPrefix = vehicleIdPrefix;
  }

  public void setRefreshInterval(int refreshInterval) {
    _refreshInterval = refreshInterval;
  }

  @PostConstruct
  public void start() {
    _executor = Executors.newSingleThreadScheduledExecutor();
    _executor.scheduleAtFixedRate(new RefreshTask(), 0, _refreshInterval,
        TimeUnit.SECONDS);
  }

  @PreDestroy
  public void stop() {
    _executor.shutdownNow();
  }

  /****
   * Private Methods
   ****/

  private void handleRecords(List<Record> records) {

    if (records.isEmpty())
      return;

    System.out.println("====== CYCLE ======");

    Map<Integer, List<Record>> recordsByVehicleId = MappingLibrary.mapToValueList(
        records, "vehicleId");

    List<VehicleLocationRecord> results = new ArrayList<VehicleLocationRecord>();

    for (Map.Entry<Integer, List<Record>> entry : recordsByVehicleId.entrySet()) {

      Integer key = entry.getKey();
      List<Record> recordsForVehicle = entry.getValue();

      AgencyAndId vehicleId = new AgencyAndId(_agencyId, _vehicleIdPrefix
          + Integer.toString(key));

      BlockInstance blockInstance = getBestBlockInstance(recordsForVehicle);

      if (blockInstance == null) {
        _log.warn("no block instance found for group: key=" + key);
        continue;
      }

      Map<AgencyAndId, Record> recordsByStop = groupRecordsByStop(entry.getValue());

      VehicleRecordCollection vehicleRecords = getVehicleRecordsForRecords(
          blockInstance, recordsByStop);

      if (vehicleRecords == null) {
        _log.warn("no records could be mapped to the block: " + vehicleId);
        continue;
      }

      VehicleState state = getVehicleRecordsAsVehicleState(blockInstance,
          vehicleRecords);

      VehicleLocationRecord vlr = getVehicleInstanceAsVehicleLocationRecord(
          vehicleId, state);
      results.add(vlr);

      dumpVehicleInstance(vehicleId, state);
    }

    if (!results.isEmpty())
      _vehicleLocationListener.handleVehicleLocationRecords(results);
  }

  private BlockInstance getBestBlockInstance(List<Record> records) {

    Record record = records.get(0);

    AgencyAndId routeId = new AgencyAndId(_agencyId, record.getRouteId());

    AgencyAndId destinationStopId = new AgencyAndId(_stopAgencyId,
        record.getDestinationTimepointId());

    long t = System.currentTimeMillis();

    List<BlockInstance> instances = _blockCalendarService.getActiveBlocksForRouteInTimeRange(
        routeId, t - 30 * 60 * 1000, t + 30 * 60 * 1000);

    List<BlockInstance> matchesDestination = new ArrayList<BlockInstance>();

    for (BlockInstance instance : instances) {

      // TODO - How do we handle a mixture of frequency and regular trips?
      if (instance.getFrequency() == null)
        continue;

      BlockConfigurationEntry blockConfig = instance.getBlock();
      List<BlockStopTimeEntry> stopTimes = blockConfig.getStopTimes();
      BlockStopTimeEntry lastBlockStopTime = stopTimes.get(stopTimes.size() - 1);
      StopTimeEntry lastStopTime = lastBlockStopTime.getStopTime();
      StopEntry lastStop = lastStopTime.getStop();
      if (lastStop.getId().equals(destinationStopId))
        matchesDestination.add(instance);
    }

    if (matchesDestination.isEmpty())
      return null;

    Min<BlockInstance> m = new Min<BlockInstance>();

    for (BlockInstance instance : matchesDestination) {
      int scheduleTime = (int) ((t - instance.getServiceDate()) / 1000);
      FrequencyEntry frequency = instance.getFrequency();
      if (frequency.getStartTime() <= scheduleTime
          && scheduleTime <= frequency.getEndTime()) {
        m.add(0, instance);
      } else {
        int a = Math.abs(scheduleTime - frequency.getStartTime());
        int b = Math.abs(scheduleTime - frequency.getEndTime());
        m.add(Math.min(a, b), instance);
      }
    }

    return m.getMinElement();
  }

  private Map<AgencyAndId, Record> groupRecordsByStop(List<Record> records) {

    Map<AgencyAndId, Record> recordsByStop = new HashMap<AgencyAndId, Record>();

    for (Record record : records) {
      AgencyAndId stopId = new AgencyAndId(_stopAgencyId,
          record.getTimepointId());
      recordsByStop.put(stopId, record);
    }

    return recordsByStop;
  }

  private VehicleRecordCollection getVehicleRecordsForRecords(
      BlockInstance blockInstance, Map<AgencyAndId, Record> recordsByStop) {

    VehicleRecordCollection vehicleRecords = null;

    for (BlockStopTimeEntry blockStopTime : blockInstance.getBlock().getStopTimes()) {

      StopTimeEntry stopTime = blockStopTime.getStopTime();
      StopEntry stop = stopTime.getStop();
      AgencyAndId stopId = stop.getId();

      Record recordForStop = recordsByStop.get(stopId);

      if (recordForStop == null)
        continue;

      if (vehicleRecords == null) {
        vehicleRecords = new VehicleRecordCollection(recordForStop,
            blockStopTime);
      } else {
        vehicleRecords.addRecord(recordForStop, blockStopTime);
      }
    }

    return vehicleRecords;
  }

  private VehicleState getVehicleRecordsAsVehicleState(
      BlockInstance blockInstance, VehicleRecordCollection vehicleRecords) {

    BlockConfigurationEntry blockConfig = blockInstance.getBlock();
    List<BlockStopTimeEntry> stopTimes = blockConfig.getStopTimes();

    Record nextRecord = vehicleRecords.getFirstRecord();
    BlockStopTimeEntry nextStopTime = vehicleRecords.getFirstBlockStopTime();

    int timeToFirstStop = (int) ((nextRecord.getTimepointTime() - nextRecord.getTime()) / 1000);
    int scheduleTime = nextStopTime.getStopTime().getArrivalTime()
        - timeToFirstStop;

    BlockStopTimeEntry firstStopTime = stopTimes.get(0);
    int firstArrival = firstStopTime.getStopTime().getArrivalTime();

    if (scheduleTime < firstArrival) {
      ScheduledBlockLocation scheduledBlockLocation = _scheduledBlockLocationService.getScheduledBlockLocationFromScheduledTime(
          blockConfig, firstArrival);
      return new VehicleState(blockInstance, scheduledBlockLocation,
          scheduleTime, EVehiclePhase.LAYOVER_BEFORE, nextRecord.getTime(),
          vehicleRecords);
    }

    ScheduledBlockLocation scheduledBlockLocation = _scheduledBlockLocationService.getScheduledBlockLocationFromScheduledTime(
        blockConfig, scheduleTime);
    return new VehicleState(blockInstance, scheduledBlockLocation,
        scheduleTime, EVehiclePhase.IN_PROGRESS, nextRecord.getTime(),
        vehicleRecords);
  }

  private VehicleLocationRecord getVehicleInstanceAsVehicleLocationRecord(
      AgencyAndId vehicleId, VehicleState state) {

    BlockInstance blockInstance = state.getBlockInstance();
    BlockConfigurationEntry blockConfig = blockInstance.getBlock();
    BlockEntry block = blockConfig.getBlock();
    ScheduledBlockLocation blockLocation = state.getBlockLocation();

    VehicleRecordCollection vehicleRecords = state.getRecords();
    List<TimepointPredictionRecord> timepointPredictions = getRecordsAsTimepointPredictions(vehicleRecords);

    VehicleLocationRecord r = new VehicleLocationRecord();
    r.setBlockId(block.getId());
    r.setPhase(state.getPhase());
    r.setServiceDate(blockInstance.getServiceDate());
    r.setDistanceAlongBlock(blockLocation.getDistanceAlongBlock());
    r.setTimeOfRecord(state.getUpdateTime());
    r.setTimepointPredictions(timepointPredictions);
    r.setVehicleId(vehicleId);

    int scheduleTime = (int) ((state.getUpdateTime() - blockInstance.getServiceDate()) / 1000);
    int scheduleDeviation = (int) (scheduleTime - state.getEffectiveScheduleTime());
    r.setScheduleDeviation(scheduleDeviation);

    return r;
  }

  private List<TimepointPredictionRecord> getRecordsAsTimepointPredictions(
      VehicleRecordCollection vehicleRecords) {

    List<Record> records = vehicleRecords.getRecords();
    List<BlockStopTimeEntry> blockStopTimes = vehicleRecords.getBlockStopTimes();

    int n = records.size();
    List<TimepointPredictionRecord> results = new ArrayList<TimepointPredictionRecord>(
        n);

    for (int i = 0; i < n; i++) {
      Record record = records.get(i);
      BlockStopTimeEntry blockStopTime = blockStopTimes.get(i);
      TimepointPredictionRecord tpr = new TimepointPredictionRecord();
      tpr.setTimepointId(blockStopTime.getStopTime().getStop().getId());
      tpr.setTimepointPredictedTime(record.getTimepointTime());
      results.add(tpr);
    }

    return results;
  }

  private void dumpVehicleInstance(AgencyAndId vehicleId, VehicleState state) {
    System.out.println("== vehicle=" + vehicleId + " ==");
    System.out.println("  effectiveScheduleTime="
        + state.getEffectiveScheduleTime());
    System.out.println("  block="
        + state.getBlockInstance().getBlock().getBlock().getId());
    System.out.println("  location=" + state.getBlockLocation().getLocation());
    System.out.println("  phase=" + state.getPhase());
    VehicleRecordCollection vehicleRecords = state.getRecords();

    for (Record record : vehicleRecords.getRecords())
      System.out.println("  " + record.getNextSign() + " "
          + _format.format(new Date(record.getTimepointTime())));
  }

  /****
   * 
   ****/

  private class RefreshTask implements Runnable, EntityHandler {

    private DefaultHttpClient _client = new DefaultHttpClient();

    private CsvEntityReader _reader = new CsvEntityReader();

    private List<Record> _records = new ArrayList<Record>();

    public RefreshTask() {

      if (_username != null) {

        Credentials credentials = null;

        if (_useNtlm) {
          _client.getAuthSchemes().register("ntlm", new NTLMSchemeFactory());
          credentials = new NTCredentials(_username, _password, "MYSERVER",
              _domain);
        } else {
          credentials = new UsernamePasswordCredentials(_username, _password);
        }

        _client.getCredentialsProvider().setCredentials(AuthScope.ANY,
            credentials);
      }

      _reader.addEntityHandler(this);
    }

    @Override
    public void run() {

      try {

        _records.clear();

        HttpGet get = new HttpGet(_url);
        HttpResponse response = _client.execute(get);
        HttpEntity entity = response.getEntity();

        if (entity != null) {
          InputStream in = entity.getContent();
          _reader.readEntities(Record.class, in);
          in.close();
        }

        handleRecords(_records);

        _records.clear();

      } catch (Exception ex) {
        _log.warn("error querying realtime data", ex);
      }
    }

    @Override
    public void handleEntity(Object obj) {

      Record record = (Record) obj;

      // Prune the record if the predicted arrival time is in the past
      if (record.getTimepointTime() < record.getTime() - 60 * 60 * 1000)
        return;

      _records.add(record);
    }
  }

}
