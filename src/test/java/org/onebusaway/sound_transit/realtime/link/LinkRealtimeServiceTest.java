package org.onebusaway.sound_transit.realtime.link;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:data-sources.xml"})
public class LinkRealtimeServiceTest {

  private LinkRealtimeService _linkRealtimeService;

  @Autowired
  public void setLinkRealtimeService(LinkRealtimeService linkRealtimeService) {
    _linkRealtimeService = linkRealtimeService;
  }

  @Test
  public void test() throws InterruptedException {

    _linkRealtimeService.start();

    Thread.sleep(10 * 60 * 1000);

    _linkRealtimeService.stop();
  }
}
