import sys

from tabulate import tabulate
import pandas as pd


def plot_optimal_functionsets(input):
    data = pd.read_csv(input, sep=";")

    ppis = data["ppi"].unique()
    timeframes = data["from"].unique()

    for ppi in ppis:
        for timeframe in timeframes:
            mask =  ((data["from"] == timeframe) & (data["ppi"] == ppi))
            selected = data.loc[mask]
            selected = selected[["ppi", "from", "functionset", "rmse"]].drop_duplicates()
            selected = selected.sort_values('functionset')

            print(tabulate(selected, headers = selected.columns, tablefmt = 'latex', showindex=False))
            print()
if __name__ == "__main__":
    plot_optimal_functionsets(sys.argv[1])
