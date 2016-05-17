package me.haolee.gp.common;

import java.io.Serializable;

public class Packet implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 8780195493574482007L;
	private CommandWord commandWord;
	private Object fields;
	public Packet(CommandWord commandWord,Object field) {
		this.commandWord = commandWord;
		this.fields = field;
	}
	public CommandWord getCommandWord() {
		return commandWord;
	}
	public Object getFields() {
		return fields;
	}
}
