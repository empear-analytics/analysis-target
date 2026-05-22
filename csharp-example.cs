class TestClass
{
    static void Main(string[] args)
    {
        if (IsTestRunCommand(args))
        {
            System.Console.WriteLine("Running tests");
        }
    }

    static bool IsTestRunCommand(string[] args)
    {
        return args.Length >= 2 && args[0] == "cli" && args[1] == "test-run";
    }
}
