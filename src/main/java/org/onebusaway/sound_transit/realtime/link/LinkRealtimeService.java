package org.onebusaway.sound_transit.realtime.link;

import java.io.InputStream;
import java.util.ArrayList;
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
import org.onebusaway.collections.FactoryMap;
import org.onebusaway.gtfs.csv.CsvEntityReader;
import org.onebusaway.gtfs.csv.EntityHandler;
import org.onebusaway.realtime.api.VehicleLocationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class LinkRealtimeService {

  private static Logger _log = LoggerFactory.getLogger(LinkRealtimeService.class);

  private VehicleLocationListener _vehicleLocationListener;

  private ScheduledExecutorService _executor;

  private int _refreshInterval = 60;

  private String _url;

  private String _username;

  private String _password;

  private boolean _useNtlm = false;

  private String _domain;

  @Autowired
  public void setVehicleLocationListener(
      VehicleLocationListener vehicleLocationListener) {
    _vehicleLocationListener = vehicleLocationListener;
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

    Map<RecordGroupingKey, List<Record>> groupedRecords = groupRecords(records);

    System.out.println(groupedRecords.size());
  }

  private Map<RecordGroupingKey, List<Record>> groupRecords(List<Record> records) {

    Map<RecordGroupingKey, List<Record>> m = new FactoryMap<RecordGroupingKey, List<Record>>(
        new ArrayList<Record>());

    for (Record record : records) {
      RecordGroupingKey key = new RecordGroupingKey(record.getLat(),
          record.getLon(), record.getDirection());
      m.get(key).add(record);
    }

    return m;
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
      _records.add((Record) obj);
    }
  }
}
