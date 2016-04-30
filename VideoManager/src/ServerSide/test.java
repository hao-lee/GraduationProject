package ServerSide;

public class test {
	public static void main(String[] args) {
		DatebaseQuery datebaseOperation = new DatebaseQuery();
		datebaseOperation.getCategory(DefineConstant.MODE_LIVE);
	}
}
