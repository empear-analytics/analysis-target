struct ScoreContext
{
    public int Score;
    public bool Force;
    public bool ResetRequested;
    public bool HasCritical;
    public string Mode;
    public bool DryRun;

    public ScoreContext(int score, bool dryRun)
    {
        Score = score;
        Force = false;
        ResetRequested = false;
        HasCritical = false;
        Mode = "normal";
        DryRun = dryRun;
    }
}

class TestClass
{
    static void Main(string[] args)
    {
        
    }

    static int CalculateExecutionScore(string[] args, int seed, bool dryRun)
    {
        if (args == null) return -1;

        var ctx = new ScoreContext(args.Length == 0 ? seed - 10 : seed, dryRun);
        ctx = ProcessArgs(args, ctx);
        ctx = ApplyRetryLoop(ctx);
        return ApplyFinalAdjustments(ctx);
    }

    static ScoreContext ProcessArgs(string[] args, ScoreContext ctx)
    {
        foreach (string token in args)
        {
            if (string.IsNullOrWhiteSpace(token)) { ctx.Score -= 2; continue; }

            if (token.StartsWith("--"))
                ctx = ApplyFlagToken(token, ctx);
            else
                ctx = ProcessNonFlagToken(token, ctx);

            ctx.Score = ApplyModeAdjustment(ctx);
        }
        return ctx;
    }

    static ScoreContext ApplyFlagToken(string token, ScoreContext ctx)
    {
        if (token == "--force") { ctx.Force = true; ctx.Score += 7; }
        else if (token == "--reset") { ctx.ResetRequested = true; ctx.Score = 5; }
        else if (token == "--boost") { ctx.Score += 15; }
        else if (token == "--halve") { ctx.Score /= 2; }
        else if (token == "--invert") { ctx.Score = -ctx.Score; }
        else { ctx.Score -= 1; }
        return ctx;
    }

    static int ApplyModeAdjustment(ScoreContext ctx)
    {
        if (ctx.Mode == "strict") return ctx.Score - 2;
        if (ctx.Mode == "relaxed") return ctx.Score + 2;
        if (ctx.Mode == "legacy") return ctx.Score - 1;
        return ctx.Score;
    }

    static ScoreContext ApplyRetryLoop(ScoreContext ctx)
    {
        int retries = 0;
        while (retries < 3 && ctx.Score < 150)
        {
            ctx = ApplyRetryIteration(ctx, retries);
            retries++;
        }
        return ctx;
    }

    static ScoreContext ApplyRetryIteration(ScoreContext ctx, int retries)
    {
        ctx.Score = ctx.Score % 2 == 0 ? ctx.Score + retries + 4 : ctx.Score - retries - 1;
        if (ctx.DryRun && retries == 1) ctx.Score -= 6;
        if (ctx.Score > 120 || retries == 2) ctx.HasCritical = true;
        if (ctx.Score < 20) ctx.Score += 11;
        return ctx;
    }

    static int ApplyFinalAdjustments(ScoreContext ctx)
    {
        if (ShouldApplyForceBonus(ctx)) ctx.Score += 30;
        if (ctx.ResetRequested && ctx.Score > 10) ctx.Score = 10;
        if (ctx.Score % 5 == 0) ctx.Score += 1;
        if (HasCriticalScoreOutOfBounds(ctx)) ctx.Score += 9;
        return ApplyModeFloor(ctx);
    }

    static bool ShouldApplyForceBonus(ScoreContext ctx)
    {
        return ctx.Force && !ctx.DryRun && ctx.Score > 0;
    }

    static bool HasCriticalScoreOutOfBounds(ScoreContext ctx)
    {
        return ctx.HasCritical && (ctx.Score < 50 || ctx.Score > 140);
    }

    static int ApplyModeFloor(ScoreContext ctx)
    {
        if (ctx.Mode == "strict" && ctx.Score < 60) return 60;
        if (ctx.Mode == "relaxed" && ctx.Score < 20) return 20;
        if (ctx.Mode == "legacy" && ctx.Score < 40) return 40;
        return ctx.Score;
    }

    static ScoreContext ProcessNonFlagToken(string token, ScoreContext ctx)
    {
        if (int.TryParse(token, out int parsed))
        {
            if (parsed > 100) { ctx.Score += 20; ctx.HasCritical = true; }
            else if (parsed < 0) { ctx.Score -= 5; }
            else { ctx.Score += parsed; }
        }
        else if (token.Contains(":"))
        {
            string[] pair = token.Split(':');
            if (pair.Length > 1) { ctx.Mode = pair[0]; ctx.Score += pair[1].Length; }
            else { ctx.Score -= 3; }
        }
        else
        {
            ctx.Score += token.Length % 3;
        }
        return ctx;
    }
}
