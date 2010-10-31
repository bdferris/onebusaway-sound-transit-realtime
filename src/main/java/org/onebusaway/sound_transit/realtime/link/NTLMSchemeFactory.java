package org.onebusaway.sound_transit.realtime.link;

import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthSchemeFactory;
import org.apache.http.impl.auth.NTLMScheme;
import org.apache.http.params.HttpParams;

class NTLMSchemeFactory implements AuthSchemeFactory {

  public AuthScheme newInstance(final HttpParams params) {
    return new NTLMScheme(new JCIFSEngine());
  }

}