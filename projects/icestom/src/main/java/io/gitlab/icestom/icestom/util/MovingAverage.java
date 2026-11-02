package io.gitlab.icestom.icestom.util;

public class MovingAverage {
    private final int period;
    private double sum;
    private int size;
    private final double[] window;
    private int index = 0;

    /**
     * Constructs a new moving average with the specified period.
     *
     * @param period the period
     */
    public MovingAverage(int period) {
        this.period = period;
        this.window = new double[period];
    }

    /**
     * Adds a new number to the series and updates the average.
     *
     * @param num the new number
     * @return the updated average
     */
    public double add(double num) {
        sum += num;
        if (size < period) {
            window[size] = num;
            size++;
        } else {
            sum -= window[index];
            window[index] = num;
            index = (index + 1) % period;
        }
        return sum / size;
    }

    /**
     * Returns the current average of the series.
     *
     * @return the current average
     */
    public double getAverage() {
        return sum / size;
    }
}