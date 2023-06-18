use serde::{de::Error, Serializer, Serialize, Deserializer, Deserialize};
use chrono::NaiveDate;
use std::str::FromStr;


//Not shure is zone needed
const FORMAT_OUT: &'static str = "%Y-%m-%d";
const FORMAT_IN: &'static str = "%Y-%m-%d";

// The signature of a serialize_with function must follow the pattern:
//
//    fn serialize<S>(&T, S) -> Result<S::Ok, S::Error>
//    where
//        S: Serializer
//
// although it may also be generic over the input types T.
pub fn serialize<S>(
    _date: &Option<NaiveDate>,
    serializer: S,
) -> Result<S::Ok, S::Error>
where
    S: Serializer,
{
    if let Some(date) = _date {
        let s = format!("{}", date.format(FORMAT_OUT));
        serializer.serialize_str(&s)
    } else {
        serializer.serialize_none()
    }
}

// The signature of a deserialize_with function must follow the pattern:
//
//    fn deserialize<'de, D>(D) -> Result<T, D::Error>
//    where
//        D: Deserializer<'de>
//
// although it may also be generic over the output types T.
pub fn deserialize<'de, D>(
    deserializer: D,
) -> Result<Option<NaiveDate>, <D as Deserializer<'de>>::Error>
where
    D: Deserializer<'de>,
{
    let s = String::deserialize(deserializer)?;
    NaiveDate::parse_from_str(&s, FORMAT_IN)
        .map_err(serde::de::Error::custom)
        .map(|r| Some(r))
}
