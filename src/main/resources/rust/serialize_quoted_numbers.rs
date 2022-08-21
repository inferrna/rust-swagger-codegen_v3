//Taken here https://earvinkayonga.com/posts/deserialize-date-in-rust/
use serde::{de::Error, Serializer, Serialize, Deserializer, Deserialize};
use std::clone::Clone;
use std::fmt::Display;
use std::str::FromStr;



// The signature of a serialize_with function must follow the pattern:
//
//    fn serialize<S>(&T, S) -> Result<S::Ok, S::Error>
//    where
//        S: Serializer
//
// although it may also be generic over the input types T.
pub fn serialize<S,T>(
    num: &T,
    serializer: S,
) -> Result<S::Ok, S::Error>
where
    S: Serializer, T: Display
{
    let s = format!("\"{}\"", num);
    serializer.serialize_str(&s)
}

// The signature of a deserialize_with function must follow the pattern:
//
//    fn deserialize<'de, D>(D) -> Result<T, D::Error>
//    where
//        D: Deserializer<'de>
//
// although it may also be generic over the output types T.
pub fn deserialize<'de, D, T>(
    deserializer: D,
) -> Result<T, <D as Deserializer<'de>>::Error>
where
    D: Deserializer<'de>, T: FromStr
{
    let s = String::deserialize(deserializer)?;
    let result: T = s.parse().map_err(|_|Error::custom(format!("Can't parse {} from \"{}\"", std::any::type_name::<T>(), s)))?;
    Ok(result)
}
