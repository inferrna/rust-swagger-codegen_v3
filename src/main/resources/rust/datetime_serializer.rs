//Taken here https://earvinkayonga.com/posts/deserialize-date-in-rust/
use serde::{de::Error, Serializer, Serialize, Deserializer, Deserialize};
use chrono::{DateTime, Utc, SecondsFormat};
use std::clone::Clone;


//Not needed. Swagger refers to https://xml2rfc.tools.ietf.org/public/rfc/html/rfc3339.html
//const FORMAT_OUT: &'static str = "%Y-%m-%dT%H:%M:%S%.f";
//const FORMAT_IN: &'static str = "%Y-%m-%dT%H:%M:%S%.f";

// The signature of a serialize_with function must follow the pattern:
//
//    fn serialize<S>(&T, S) -> Result<S::Ok, S::Error>
//    where
//        S: Serializer
//
// although it may also be generic over the input types T.
pub fn serialize_dt<S>(
    date: &DateTime<Utc>,
    serializer: S,
) -> Result<S::Ok, S::Error>
where
    S: Serializer,
{
    let s = format!("{}", date.to_rfc3339_opts(SecondsFormat::Secs, false));
    serializer.serialize_str(&s)
}

// The signature of a deserialize_with function must follow the pattern:
//
//    fn deserialize<'de, D>(D) -> Result<T, D::Error>
//    where
//        D: Deserializer<'de>
//
// although it may also be generic over the output types T.
pub fn deserialize_dt<'de, D>(
    deserializer: D,
) -> Result<DateTime<Utc>, <D as Deserializer<'de>>::Error>
where
    D: Deserializer<'de>,
{
    let s = String::deserialize(deserializer)?;
    DateTime::parse_from_rfc3339(&s).map_err(serde::de::Error::custom).map(|r| r.with_timezone(&Utc))
}
