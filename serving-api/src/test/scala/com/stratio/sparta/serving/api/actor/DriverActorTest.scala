/*
 * Copyright (C) 2015 Stratio (http://stratio.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stratio.sparta.serving.api.actor

import java.nio.file.{Files, Path}

import akka.actor.{ActorSystem, Props}
import akka.testkit.{DefaultTimeout, ImplicitSender, TestKit}
import akka.util.Timeout
import com.stratio.sparta.serving.api.actor.DriverActor.UploadDrivers
import com.stratio.sparta.serving.core.config.{SpartaConfig, SpartaConfigFactory}
import com.stratio.sparta.serving.core.models.SpartaSerializer
import com.stratio.sparta.serving.core.models.files.{SpartaFile, SpartaFilesResponse}
import com.typesafe.config.{Config, ConfigFactory}
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import spray.http.BodyPart

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

@RunWith(classOf[JUnitRunner])
class DriverActorTest extends TestKit(ActorSystem("PluginActorSpec"))
  with DefaultTimeout
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with MockitoSugar with SpartaSerializer {

  val tempDir: Path = Files.createTempDirectory("test")
  tempDir.toFile.deleteOnExit()

  val localConfig: Config = ConfigFactory.parseString(
    s"""
       |sparta{
       |   api {
       |     host = local
       |     port= 7777
       |   }
       |}
       |
       |sparta.config.driverPackageLocation = "$tempDir"
    """.stripMargin)

  val fileList = Seq(BodyPart("reference.conf", "file"))

  override def beforeEach(): Unit = {
    SpartaConfig.initMainConfig(Option(localConfig), SpartaConfigFactory(localConfig))
    SpartaConfig.initApiConfig()
  }

  override def afterAll: Unit = {
    shutdown()
  }

  override implicit val timeout: Timeout = Timeout(15 seconds)

  "DriverActor " must {

    "Not save files with wrong extension" in {
      val driverActor = system.actorOf(Props(new DriverActor()))
      driverActor ! UploadDrivers(fileList)
      expectMsgPF() {
        case SpartaFilesResponse(Success(f: Seq[SpartaFile])) => f.isEmpty shouldBe true
      }
    }
    "Not upload empty files" in {
      val driverActor = system.actorOf(Props(new DriverActor()))
      driverActor ! UploadDrivers(Seq.empty)
      expectMsgPF() {
        case SpartaFilesResponse(Failure(f)) => f.getMessage shouldBe "At least one file is expected"
      }
    }
    "Save a file" in {
      val driverActor = system.actorOf(Props(new DriverActor()))
      driverActor ! UploadDrivers(Seq(BodyPart("reference.conf", "file.jar")))
      expectMsgPF() {
        case SpartaFilesResponse(Success(f: Seq[SpartaFile])) => f.head.fileName.endsWith("file.jar") shouldBe true
      }
    }
  }
}
