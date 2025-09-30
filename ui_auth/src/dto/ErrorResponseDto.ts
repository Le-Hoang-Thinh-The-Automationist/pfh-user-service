type ISODateString = string;

export interface FieldErrorDto {
  field: string;
  message: string;
}

export interface ErrorResponseDto {
  status: number;
  message: string;
  timestamp: ISODateString;
  errors: FieldErrorDto[];
}
