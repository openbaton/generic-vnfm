package org.openbaton.vnfm.generic;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/** Created by ogo on 30.08.17. */
@RestController
@RequestMapping("/api/v1/download")
public class EmsProvider {
  @Value("${vnfm.ems.debian.path:/opt/openbaton/ems/ems-0.22.deb}")
  private String emsPathDebian;
  @Value("${vnfm.ems.rpm.path:/opt/openbaton/ems/ems.rpm}")
  private String emsPathRpm;

  @RequestMapping(value = "ems.deb", method = RequestMethod.GET)
  public ResponseEntity fetchEmsDebian() {
    FileInputStream fi;
    try {
      fi = new FileInputStream(emsPathDebian);
    } catch (FileNotFoundException e) {
      return new ResponseEntity<>(
          "EMS debian package does not exist in the path that is set in properties",
          HttpStatus.FAILED_DEPENDENCY);
    }
    return ResponseEntity.ok()
        .header("Accept-Ranges", "bytes")
        .eTag("\"" + "ems.deb" + "\"")
        .contentType(MediaType.parseMediaType("application/octet-stream"))
        .header("content-disposition", "attachment; filename=\"" + "ems.deb" + "\"")
        .body(new InputStreamResource(fi));
  }
  @RequestMapping(value = "ems.rpm", method = RequestMethod.GET)
  public ResponseEntity fetchEmsRPM() {
    FileInputStream fi;
    try {
      fi = new FileInputStream(emsPathRpm);
    } catch (FileNotFoundException e) {
      return new ResponseEntity<>(
              "EMS rpm package does not exist in the path that is set in properties",
              HttpStatus.FAILED_DEPENDENCY);
    }
    return ResponseEntity.ok()
            .header("Accept-Ranges", "bytes")
            .eTag("\"" + "ems.rpm" + "\"")
            .contentType(MediaType.parseMediaType("application/octet-stream"))
            .header("content-disposition", "attachment; filename=\"" + "ems.rpm" + "\"")
            .body(new InputStreamResource(fi));
  }
}
