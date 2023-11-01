rm results.txt

for i in {1..1}
do
    OUTPUT="$(./checkers trainer.sh trainer.sh 3 | tail -20)"
    echo $OUTPUT | grep -q 'Player 2 has lost the game' && echo 'Win' >> results.txt
    echo $OUTPUT | grep -q 'Player 1 has lost the game' && echo 'Lost' >> results.txt
    echo $OUTPUT | grep -q 'Draw' && echo 'Draw' >> results.txt

    player1Win="0.5"
    if [[ $OUTPUT == *"Player 2 has lost the game"* ]]
    then
        player1Win="1.0"
    elif [[ $OUTPUT == *"Player 1 has lost the game"* ]]
    then
        player1Win="0.0"
    fi

    sed -e "s/$/,${player1Win}/" -i tmp.csv

    echo "" >> training.csv
    cat tmp.csv >> training.csv
    rm -f tmp.csv

    java -cp target/rl-checkers-1.0-SNAPSHOT-jar-with-dependencies.jar WaffleNNModel

    killall -9 java
done
