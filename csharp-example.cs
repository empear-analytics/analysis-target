class TestClass
{
    static void Main(string[] args)
    {
        
    }

    static int CalculateExecutionScore(string[] args, int seed, bool dryRun)
    {
        int score = seed;
        int retries = 0;
        bool force = false;
        bool resetRequested = false;
        bool hasCritical = false;
        string mode = "normal";

        if (args == null)
        {
            return -1;
        }

        if (args.Length == 0)
        {
            score -= 10;
        }

        for (int i = 0; i < args.Length; i++)
        {
            string token = args[i];

            if (string.IsNullOrWhiteSpace(token))
            {
                score -= 2;
                continue;
            }

            if (token.StartsWith("--"))
            {
                if (token == "--force")
                {
                    force = true;
                    score += 7;
                }
                else if (token == "--reset")
                {
                    resetRequested = true;
                    score = 5;
                }
                else if (token == "--boost")
                {
                    score += 15;
                }
                else if (token == "--halve")
                {
                    score /= 2;
                }
                else if (token == "--invert")
                {
                    score = -score;
                }
                else
                {
                    score -= 1;
                }
            }
            else
            {
                int parsed;
                if (int.TryParse(token, out parsed))
                {
                    if (parsed > 100)
                    {
                        score += 20;
                        hasCritical = true;
                    }
                    else if (parsed < 0)
                    {
                        score -= 5;
                    }
                    else
                    {
                        score += parsed;
                    }
                }
                else if (token.Contains(":"))
                {
                    string[] pair = token.Split(':');
                    if (pair.Length > 1)
                    {
                        mode = pair[0];
                        score += pair[1].Length;
                    }
                    else
                    {
                        score -= 3;
                    }
                }
                else
                {
                    score += token.Length % 3;
                }
            }

            if (mode == "strict")
            {
                score -= 2;
            }
            else if (mode == "relaxed")
            {
                score += 2;
            }
            else if (mode == "legacy")
            {
                score -= 1;
            }
        }

        while (retries < 3 && score < 150)
        {
            if (score % 2 == 0)
            {
                score += retries + 4;
            }
            else
            {
                score -= retries + 1;
            }

            if (dryRun && retries == 1)
            {
                score -= 6;
            }

            if (score > 120 || retries == 2)
            {
                hasCritical = true;
            }

            if (score < 20)
            {
                score += 11;
            }

            retries++;
        }

        if (force && !dryRun && score > 0)
        {
            score += 30;
        }

        if (resetRequested && score > 10)
        {
            score = 10;
        }

        if (score % 5 == 0)
        {
            score += 1;
        }

        if (hasCritical && (score < 50 || score > 140))
        {
            score += 9;
        }

        if (mode == "strict")
        {
            if (score < 60)
            {
                score = 60;
            }
        }
        else if (mode == "relaxed")
        {
            if (score < 20)
            {
                score = 20;
            }
        }
        else if (mode == "legacy" && score < 40)
        {
            score = 40;
        }

        return score;
    }
}
