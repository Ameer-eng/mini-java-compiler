/*
 * Test program
 */
class Test {
    public static void main(String [] args) {
        int x = fib(6) * fib(9);
        System.out.println(x);
    }

    public int fib(int n) {
        if (n == 0) {
            return 0;
        }
        if (n == 1) {
            return 1;
        }
        return fib(n - 1) + fib(n - 2);
    }
}
