/*
 * Copyright (C) 2017-2021 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package com.here.platform.example.location.java.standalone;

import com.here.platform.location.core.graph.javadsl.PropertyMap;
import com.here.platform.location.dataloader.core.Catalog;
import com.here.platform.location.dataloader.core.caching.CacheManager;
import com.here.platform.location.dataloader.standalone.StandaloneCatalogFactory;
import com.here.platform.location.inmemory.graph.Vertex;
import com.here.platform.location.inmemory.graph.javadsl.Direction;
import com.here.platform.location.integration.optimizedmap.OptimizedMap;
import com.here.platform.location.integration.optimizedmap.geospatial.HereMapContentReference;
import com.here.platform.location.integration.optimizedmap.graph.javadsl.PropertyMaps;
import com.here.platform.location.referencing.LinearLocation;
import com.here.platform.location.referencing.LocationReferenceResolver;
import com.here.platform.location.referencing.ReferencingLocation;
import com.here.platform.location.referencing.javadsl.LocationReferenceResolvers;
import com.here.platform.location.tpeg2.XmlMarshallers;
import com.here.platform.location.tpeg2.olr.OpenLRLocationReference;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This example shows how to take an OLR reference given in XML and to resolve this reference to
 * Here Map Content Reference.
 */
public final class OlrResolveReferenceToHmcSegmentsExample {

  public static void main(final String[] args) {
    final StandaloneCatalogFactory factory = new StandaloneCatalogFactory();
    final Catalog optimizedMap = factory.create(OptimizedMap.v2.HRN, 769L);

    final CacheManager cacheManager = CacheManager.withLruCache();

    try {
      final String referenceXml =
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
              + "<olr:OpenLRLocationReference xmlns:olr=\"http://www.tisa.org/TPEG/OLR_0_1\">\n"
              + "    <olr:version>1.1</olr:version>\n"
              + "    <olr:locationReference>\n"
              + "        <olr:optionLinearLocationReference>\n"
              + "            <olr:first>\n"
              + "                <olr:coordinate>\n"
              + "                    <olr:longitude>623039</olr:longitude>\n"
              + "                    <olr:latitude>2447911</olr:latitude>\n"
              + "                </olr:coordinate>\n"
              + "                <olr:lineProperties>\n"
              + "                    <olr:frc olr:table=\"olr001_FunctionalRoadClass\" olr:code=\"2\"></olr:frc>\n"
              + "                    <olr:fow olr:table=\"olr002_FormOfWay\" olr:code=\"2\"></olr:fow>\n"
              + "                    <olr:bearing>\n"
              + "                        <olr:value>174</olr:value>\n"
              + "                    </olr:bearing>\n"
              + "                </olr:lineProperties>\n"
              + "                <olr:pathProperties>\n"
              + "                    <olr:lfrcnp olr:table=\"olr001_FunctionalRoadClass\" olr:code=\"1\"></olr:lfrcnp>\n"
              + "                    <olr:dnp>\n"
              + "                        <olr:value>4649</olr:value>\n"
              + "                    </olr:dnp>\n"
              + "                    <olr:againstDrivingDirection>false</olr:againstDrivingDirection>\n"
              + "                </olr:pathProperties>\n"
              + "            </olr:first>\n"
              + "            <olr:last>\n"
              + "                <olr:coordinate>\n"
              + "                    <olr:longitude>-3598</olr:longitude>\n"
              + "                    <olr:latitude>-1748</olr:latitude>\n"
              + "                </olr:coordinate>\n"
              + "                <olr:lineProperties>\n"
              + "                    <olr:frc olr:table=\"olr001_FunctionalRoadClass\" olr:code=\"1\"></olr:frc>\n"
              + "                    <olr:fow olr:table=\"olr002_FormOfWay\" olr:code=\"3\"></olr:fow>\n"
              + "                    <olr:bearing>\n"
              + "                        <olr:value>144</olr:value>\n"
              + "                    </olr:bearing>\n"
              + "                </olr:lineProperties>\n"
              + "            </olr:last>\n"
              + "        </olr:optionLinearLocationReference>\n"
              + "    </olr:locationReference>\n"
              + "</olr:OpenLRLocationReference>\n";

      final PropertyMap<Vertex, HereMapContentReference> vertexToHmc =
          PropertyMaps.vertexToHereMapContentReference(optimizedMap, cacheManager);

      final OpenLRLocationReference reference =
          XmlMarshallers.openLRLocationReference()
              .unmarshall(new ByteArrayInputStream(referenceXml.getBytes(StandardCharsets.UTF_8)));

      final LocationReferenceResolver<OpenLRLocationReference, ReferencingLocation> resolver =
          LocationReferenceResolvers.olr(optimizedMap, cacheManager);
      final ReferencingLocation location = resolver.resolve(reference);

      // OLR supports multiple types of location references.
      // If we use the universal OLR resolver (olr(…)), we need to
      // check which subtype of `ReferencingLocation` we actually get back.
      if (location instanceof LinearLocation) {
        final LinearLocation linearLocation = (LinearLocation) location;
        final List<HereMapContentReference> segments =
            linearLocation.getPath().stream().map(vertexToHmc::get).collect(Collectors.toList());
        segments.forEach(segment -> System.out.println(toHmcRefString(segment)));
      } else {
        System.out.println("This example only supports linear location references.");
      }
    } finally {
      factory.terminate();
    }
  }

  static String toHmcRefString(final HereMapContentReference hmcRef) {
    return hmcRef.partitionId()
        + "/"
        + hmcRef.segmentId().replaceFirst("here:cm:segment:", "")
        + (hmcRef.direction() == Direction.FORWARD ? "+" : "-");
  }
}
