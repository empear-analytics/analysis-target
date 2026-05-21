class TestClass
{
    static void Main(string[] args)
    {
        if (args.Length >= 2 && args[0] == "cli" && args[1] == "test-run")
        {
            System.Console.WriteLine("Running tests");
        }
    }
}
