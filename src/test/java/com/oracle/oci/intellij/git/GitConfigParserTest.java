package com.oracle.oci.intellij.git;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;

import com.oracle.oci.intellij.ui.git.config.GitConfig;
import com.oracle.oci.intellij.ui.git.config.GitConfigBranch;
import com.oracle.oci.intellij.ui.git.config.GitConfigCore;
import com.oracle.oci.intellij.ui.git.config.GitConfigRemote;
import com.oracle.oci.intellij.ui.git.config.GitParser;
import com.oracle.oci.intellij.ui.git.config.GitParser.GitParserException;

public class GitConfigParserTest {

  @Test
  public void testConfig() throws IOException, GitParserException {
    try (InputStream resourceAsStream =
      GitConfigParserTest.class.getClassLoader().getResourceAsStream("config");
         BufferedInputStream bis = new BufferedInputStream(resourceAsStream);
         Reader reader = new InputStreamReader(bis)) {

      GitParser parser = new GitParser();
      GitConfig config = parser.parse(reader);
      Optional.ofNullable(config)
              .ifPresent(c -> System.out.println(c.toString()));

      assertNotNull(config);

      GitConfigCore configCore = config.getConfigCore();
      assertNotNull(configCore);
      Map<String, String> actualMap = new HashMap<>(configCore.getValues());
      Map<String, String> expectedMap =
        Map.of("repositoryformatversion", "0", "filemode", "true", "bare",
               "false", "logallrefupdates", "false", "ignorecase", "true",
               "precomposeunicode", "true");
      actualMap.entrySet().forEach(e -> {
        assertTrue("Expected key: " + e.getKey(),
                     actualMap.containsKey(e.getKey()));
        assertEquals("Expected for key: " + e.getKey(),
                     actualMap.get(e.getKey()), e.getValue());
      });
      assertEquals(expectedMap.size(), actualMap.size());
      
      
      Map<String, GitConfigRemote>  remotes = config.getRemotes();
      assertEquals(remotes != null ? remotes.entrySet().toString() : "", 
           3, remotes.size());
      GitConfigRemote remote = remotes.get("origin");
      assertNotNull(remote);
      assertEquals("https://bitbucket.org/dancioca/dbn.git", remote.getUrl());
      assertEquals("+refs/heads/*:refs/remotes/origin/*", remote.getFetch());
      
      remote = remotes.get("dbncontrib");
      assertNotNull(remote);
      assertEquals("ssh://git@bitbucket.oci.oraclecorp.com:7999/dbjav/dbncontrib.git", remote.getUrl());
      assertEquals("+refs/heads/*:refs/remotes/dbncontrib/*", remote.getFetch());
      
      remote = remotes.get("dbncontrib_new2");
      assertNotNull(remote);
      assertEquals("ssh://git@bitbucket.oci.oraclecorp.com:7999/dbjav/dbncontrib_new2.git", remote.getUrl());
      assertEquals("+refs/heads/*:refs/remotes/dbncontrib_new2/*", remote.getFetch());
      
      
      Map<String, GitConfigBranch> branches = config.getBranches();
      assertEquals(3, branches.size());
      
      GitConfigBranch branch = branches.get("master");
      assertNotNull(branch);
      assertEquals("origin", branch.getRemote());
      assertEquals("refs/heads/master", branch.getValue("merge"));
      
      branch = branches.get("cbateman/review/first_changes_tns_url");
      assertNotNull(branch);
      assertEquals("dbncontrib", branch.getRemote());
      assertEquals("refs/heads/cbateman/review/first_changes_tns_url", branch.getValue("merge"));
      
      branch = branches.get("cbateman/first_changes_tns_url");
      assertNotNull(branch);
      assertEquals("dbncontrib", branch.getRemote());
      assertEquals("refs/heads/cbateman/first_changes_tns_url", branch.getValue("merge"));
     
      Set<String> urls = config.getUrls();
      assertEquals(3, urls.size());
      assertEquals(Set.of("https://bitbucket.org/dancioca/dbn.git", 
                          "ssh://git@bitbucket.oci.oraclecorp.com:7999/dbjav/dbncontrib.git", 
                          "ssh://git@bitbucket.oci.oraclecorp.com:7999/dbjav/dbncontrib_new2.git"),
                   urls);
      
    }
  }
}