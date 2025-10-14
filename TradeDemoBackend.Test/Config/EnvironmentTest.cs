using Microsoft.AspNetCore.Builder;

namespace TradeDemoBackend.Test.Config;

public class EnvironmentTest
{

   [Fact]
   public void IsNotDevModeByDefault()
   { 
       var builder = WebApplication.CreateEmptyBuilder(new WebApplicationOptions());
       var isDev = TradeDemoBackend.Config.Environment.IsDevMode(builder);
       Assert.False(isDev);
   }
}
