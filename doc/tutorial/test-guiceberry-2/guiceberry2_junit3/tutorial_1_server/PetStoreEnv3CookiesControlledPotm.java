package guiceberry2_junit3.tutorial_1_server;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.testing.guiceberry.GuiceBerryEnvMain;
import com.google.inject.testing.guiceberry.TestId;
import com.google.inject.testing.guiceberry.controllable.TestIdServerModule;
import com.google.inject.testing.guiceberry.junit3.GuiceBerryJunit3Env;

import guiceberry2_junit3.tutorial_1_server.prod.MyPetStoreServer;
import guiceberry2_junit3.tutorial_1_server.prod.PetOfTheMonth;
import guiceberry2_junit3.tutorial_1_server.prod.PortNumber;

import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;


import java.util.Map;

public final class PetStoreEnv3CookiesControlledPotm extends GuiceBerryJunit3Env {
  
  @Provides
  @PortNumber
  int getPortNumber(MyPetStoreServer server) {
    return server.getPortNumber();
  }
  
  @Provides
  WebDriver getWebDriver(@PortNumber int portNumber, TestId testId) {
    WebDriver driver = new HtmlUnitDriver();
    // !!! HERE !!!
    driver.get("http://localhost:" + portNumber);
    driver.manage().addCookie(
        new Cookie(TestId.COOKIE_NAME, testId.toString()));
    return driver;
  }

  @Provides
  @Singleton
  MyPetStoreServer buildPetStoreServer() {
    MyPetStoreServer result = new MyPetStoreServer(8080) {
      @Override
      protected Module getPetStoreModule() {
        return new PetStoreModuleWithTestIdBasedOverride();
      }
    };
    return result;
  }
  
  @Override
  protected void configure() {
    super.configure();
    bind(GuiceBerryEnvMain.class).to(PetStoreServerStarter.class);
  }
  
  private static final class PetStoreServerStarter implements GuiceBerryEnvMain {

    @Inject
    private MyPetStoreServer myPetStoreServer;
    
    public void run() {
      // Starting a server should never be done in a @Provides method 
      // (or inside Provider's get).
      PetStoreModuleWithTestIdBasedOverride.serverInjector = myPetStoreServer.start();
    }
  }

  public static final class PetStoreModuleWithTestIdBasedOverride 
      extends MyPetStoreServer.PetStoreModule {

    private static Injector serverInjector;
    
    public static final Map<TestId, PetOfTheMonth> override = Maps.newHashMap();

    // !!!HERE!!!!
    @Override
    protected PetOfTheMonth getPetOfTheMonth() {
      TestId testId = serverInjector.getInstance(TestId.class);
      PetOfTheMonth petOfTheMonth = override.get(testId);
      if (petOfTheMonth != null) {
        return petOfTheMonth;
      }
      return somePetOfTheMonth();
    }

    @Override
    protected void configure() {
      super.configure();
      install(new TestIdServerModule());
    }
  }
}