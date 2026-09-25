package com.personai.stage0

import org.junit.Assert.assertNotNull
import org.junit.Test

class PackageScaffoldingTest {

  @Test
  fun personAiPackagesHaveStage0Markers() {
    val markerClasses = listOf(
        "com.personai.agent.AgentPackageMarker",
        "com.personai.memory.MemoryPackageMarker",
        "com.personai.persona.PersonaPackageMarker",
        "com.personai.notification.NotificationPackageMarker",
        "com.personai.overlay.OverlayPackageMarker",
        "com.personai.sync.SyncPackageMarker",
        "com.personai.match.MatchPackageMarker",
        "com.personai.quantum.QuantumPackageMarker",
        "com.personai.wiki.WikiPackageMarker",
        "com.personai.pdfreasoner.PdfreasonerPackageMarker",
        "com.personai.build.BuildPackageMarker",
        "com.personai.mcp.McpPackageMarker",
        "com.personai.ratelimit.RatelimitPackageMarker",
        "com.personai.hitl.HitlPackageMarker",
    )

    markerClasses.forEach { className ->
      assertNotNull(Class.forName(className))
    }
  }

  @Test
  fun originalGalleryContractClassesRemainResolvable() {
    val protectedGalleryClasses = listOf(
        "com.google.ai.edge.gallery.MainActivity",
        "com.google.ai.edge.gallery.GalleryApplication",
        "com.google.ai.edge.gallery.ui.navigation.GalleryNavGraph",
    )

    protectedGalleryClasses.forEach { className ->
      assertNotNull(Class.forName(className))
    }
  }
}
