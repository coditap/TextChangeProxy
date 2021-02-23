package main.java;

import java.io.IOException;

public interface ProxyInputStream
{
  /** reads the data */
  public int read_f(byte[] b) throws IOException;
}
