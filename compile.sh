mvn clean package

mv target/classes/AB* ./

chmod +x trainer.sh
chmod +x waffle.sh

rm -f training.csv
rm -r tmp.csv
rm -rf model/

mkdir model/