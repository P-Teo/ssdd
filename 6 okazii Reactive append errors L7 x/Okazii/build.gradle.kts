plugins {
    kotlin("jvm") version "2.0.21"
}

group = "ro.unibuc.okazii"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Dependențele reactive necesare
    implementation("io.reactivex.rxjava3:rxjava:3.1.8")
    implementation("io.reactivex.rxjava3:rxkotlin:3.0.1")
    implementation("org.reactivestreams:reactive-streams:1.0.4")
    implementation(kotlin("stdlib"))
}

// CORECTURĂ: Îi spunem lui Gradle să caute codul Kotlin în TOATE folderele tale de microservicii
sourceSets {
    main {
        kotlin {
            setSrcDirs(
                listOf(
                    "AuctioneerMicroservice/src",
                    "BidderMicroservice/src",
                    "BiddingProcessorMicroservice/src",
                    "ErrorKeeperService/src",
                    "MessageLibrary/src",
                    "MessageProcessorMicroservice/src"
                )
            )
        }
    }
}