package test;

public class Test
{
	public static native int add(int a, int b);

	public static class ValueNotFound extends Exception
	{
		public ValueNotFound(String message)
		{
			super(message);
		}
	}
}
