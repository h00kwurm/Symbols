package org.dits.symbols;

public class InputOutput {
	
	private String input;
	private String output;
	private boolean terminated;
	private boolean isError;
	
	public InputOutput()
	{
		input = new String();
		output = new String();
		terminated = true;
		isError = false;
	}
	
	public InputOutput(String i)
	{
		input = i;
		output = new String();
		terminated = true;
		isError = false;
	}
	
	public InputOutput(String i, String o)
	{
		input = i;
		output = o;
		terminated = true;
		isError = false;
	}
	
	public void setTermination(boolean t)
	{
		this.terminated = t;
	}
	
	public boolean isTerminated()
	{
		return this.terminated;
	}
	
	public void setError(boolean k)
	{
		this.isError = k;
	}
	
	public String getInput()
	{
		return this.input;
	}
	
	public String getOutput()
	{
		return this.output;
	}
	
	public boolean getError()
	{
		return this.isError;
	}
	
	public void setOutput(String output)
	{
		this.output = output;
	}
	
	public String toString()
	{
		return (this.terminated) ? this.input : this.input + "\n\t\t>>" + this.output;
	}

}
