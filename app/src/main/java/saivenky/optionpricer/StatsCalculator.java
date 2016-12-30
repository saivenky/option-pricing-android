package saivenky.optionpricer;

public class StatsCalculator {
    public Stats calculateStandardDeviation(double[] nums) {
        Stats stats = new Stats();
        double sum = 0.0;
        for (double num : nums) {
            sum += num;
        }

        stats.average = sum / nums.length;

        double diffSquaredSum = 0.0;
        for (double num : nums) {
            diffSquaredSum += Math.pow(stats.average - num, 2);
        }

        stats.standardDeviation = Math.sqrt(diffSquaredSum / nums.length);
        return stats;
    }
}
