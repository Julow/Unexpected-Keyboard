package juloo.keyboard2;

import android.content.SharedPreferences;
import android.content.res.Resources;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import juloo.keyboard2.dict.Dictionaries;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import static org.junit.Assert.*;

public class ConfigTest
{
  final Map<String, Object> preferences = new HashMap<String, Object>();
  final Resources resources = new Resources(null, null, null);

  Config create() throws Exception
  {
    SharedPreferences prefs = (SharedPreferences)Proxy.newProxyInstance(
        SharedPreferences.class.getClassLoader(), new Class<?>[]{SharedPreferences.class},
        (proxy, method, args) -> {
          if (method.getName().startsWith("get") && args != null && args.length == 2)
            return preferences.getOrDefault(args[0], args[1]);
          throw new AssertionError("Unexpected preference access: " + method.getName());
        });
    // Exercise the real constructor/refresh without initializing global layout
    // resources. Keep this test fixture out of production Config.
    Constructor<Config> ctor = Config.class.getDeclaredConstructor(
        SharedPreferences.class, Resources.class, Boolean.class, Dictionaries.class);
    ctor.setAccessible(true);
    return ctor.newInstance(prefs, resources, false, null);
  }

  @Test
  public void sound_defaults_off_and_refreshes_independently_of_vibration() throws Exception
  {
    Config config = create();
    assertFalse(config.sound_on_keypress);
    assertEquals(50, config.sound_volume);
    boolean vibrate = config.vibrate_custom;
    long duration = config.vibrate_duration;
    preferences.put("sound_on_keypress", true);
    preferences.put("sound_volume", 75);
    config.refresh(resources, false, null);
    assertTrue(config.sound_on_keypress);
    assertEquals(75, config.sound_volume);
    assertEquals(vibrate, config.vibrate_custom);
    assertEquals(duration, config.vibrate_duration);
    preferences.put("sound_on_keypress", false);
    preferences.put("vibrate_custom", true);
    preferences.put("vibrate_duration", 42);
    config.refresh(resources, false, null);
    assertFalse(config.sound_on_keypress);
    assertTrue(config.vibrate_custom);
    assertEquals(42, config.vibrate_duration);
  }

  @Test
  public void preference_defaults_off_without_vibration_dependency() throws Exception
  {
    NodeList nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder()
      .parse(new File("res/xml/settings.xml")).getElementsByTagName("CheckBoxPreference");
    int found = 0;
    for (int i = 0; i < nodes.getLength(); i++)
    {
      Element element = (Element)nodes.item(i);
      if (element.getAttribute("android:key").equals("sound_on_keypress"))
      {
        found++;
        assertEquals("false", element.getAttribute("android:defaultValue"));
        assertFalse(element.hasAttribute("android:dependency"));
      }
    }
    assertEquals(1, found);
  }

  @Test
  public void volume_preference_has_matching_default_range_and_sound_dependency() throws Exception
  {
    NodeList nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder()
      .parse(new File("res/xml/settings.xml"))
      .getElementsByTagName("juloo.keyboard2.prefs.IntSlideBarPreference");
    int found = 0;
    for (int i = 0; i < nodes.getLength(); i++)
    {
      Element element = (Element)nodes.item(i);
      if (element.getAttribute("android:key").equals("sound_volume"))
      {
        found++;
        assertEquals(Integer.toString(create().sound_volume),
            element.getAttribute("android:defaultValue"));
        assertEquals("0", element.getAttribute("min"));
        assertEquals("100", element.getAttribute("max"));
        assertEquals("sound_on_keypress", element.getAttribute("android:dependency"));
      }
    }
    assertEquals(1, found);
  }
}
