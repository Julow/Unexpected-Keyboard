package juloo.keyboard2;

public enum NumberLayout {
  PIN,
  NUMBER,
  NORMAL;

  public static NumberLayout of_string(String name)
  {
    switch (name)
    {
      case "number": return NUMBER;
      case "normal": return NORMAL;
      case "pin": default: return PIN;
    }
  }
}
